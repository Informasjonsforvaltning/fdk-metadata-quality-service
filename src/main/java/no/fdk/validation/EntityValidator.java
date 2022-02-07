package no.fdk.validation;

import lombok.RequiredArgsConstructor;
import no.fdk.model.ValidationResult;
import no.fdk.rdf.MQA;
import no.fdk.service.MediaTypeService;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.graph.NodeFactory;
import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.RDFNode;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.util.ModelUtils;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.RDF;
import org.topbraid.shacl.validation.ValidationUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;
import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public abstract class EntityValidator implements Validator {

    private final MediaTypeService mediaTypeService;

    private Mono<Model> shapesModel;

    @Override
    public boolean supports(Model model) {
        return model
            .listResourcesWithProperty(RDF.type, getResource())
            .hasNext();
    }

    @Override
    public Mono<ValidationResult> validate(Model model) {
        return Mono.just(model)
            .zipWith(getShapesModel())
            .map(tuple -> ValidationUtil.validateModel(tuple.getT1(), tuple.getT2(), true))
            .map(Resource::getModel)
            .map(validationModel -> ValidationResult.create(model, validationModel));
    }

    protected abstract Resource getResource();

    protected abstract String getShapesPath();

    private Mono<Model> getShapesModel() {
        return Flux.merge(
            Mono.just(RDFDataMgr.loadGraph("shapes/constraints.ttl")),
            Mono.just(RDFDataMgr.loadGraph(getShapesPath())),
            getMediaTypesGraph()
        )
            .collectList()
            .map(list -> list.toArray(Graph[]::new))
            .map(MultiUnion::new)
            .map(ModelFactory::createModelForGraph);
    }

    private Mono<Graph> getMediaTypesGraph() {
        return Mono.just(ModelFactory.createDefaultModel())
            .map(model -> model.setNsPrefixes(PrefixMapping.Standard))
            .map(model -> model.setNsPrefix(MQA.PREFIX, MQA.NS))
            .zipWith(getMediaTypeNodes())
            .map(tuple -> {
                Resource list = tuple.getT1().createList(tuple.getT2())
                    .asResource()
                    .addProperty(RDF.type, RDF.List);

                ResourceUtils.renameResource(list, MQA.uri("MediaTypes"));

                return tuple.getT1();
            })
            .map(Model::getGraph);
    }

    private Mono<RDFNode[]> getMediaTypeNodes() {
        return mediaTypeService
            .getMediaTypes()
            .retryWhen(Retry.fixedDelay(Long.MAX_VALUE, Duration.ofSeconds(2)))
            .flatMapIterable(mediaType -> {
                Set<Node> nodes = new HashSet<>();

                if (mediaType.getCode() != null) {
                    nodes.add(NodeFactory.createLiteral(mediaType.getCode()));
                }

                if (mediaType.getUri() != null) {
                    nodes.add(NodeFactory.createURI(mediaType.getUri()));
                }

                return nodes;
            })
            .map(ModelUtils::convertGraphNodeToRDFNode)
            .collectList()
            .map(list -> list.toArray(RDFNode[]::new));
    }

}
