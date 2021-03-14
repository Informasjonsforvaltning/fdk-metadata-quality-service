package no.fdk.validation;

import lombok.RequiredArgsConstructor;
import no.fdk.rdf.FDK;
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
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shared.PrefixMapping;
import org.apache.jena.sparql.util.ModelUtils;
import org.apache.jena.util.ResourceUtils;
import org.apache.jena.vocabulary.RDF;
import org.topbraid.shacl.validation.ValidationUtil;
import reactor.core.publisher.Flux;

import java.util.HashSet;
import java.util.Set;

@RequiredArgsConstructor
public abstract class EntityValidator implements Validator {

    private final MediaTypeService mediaTypeService;

    @Override
    public boolean supports(Graph graph) {
        return ModelFactory
            .createModelForGraph(graph)
            .listResourcesWithProperty(RDF.type, getResource())
            .hasNext();
    }

    @Override
    public ValidationReport validate(Graph graph) {
        Model model = ModelFactory.createModelForGraph(graph);
        Resource report = ValidationUtil.validateModel(model, getShapesModel(), true);

        return ValidationReport.fromModel(report.getModel());
    }

    protected abstract Resource getResource();

    protected abstract String getShapesPath();

    private Model getShapesModel() {
        Graph constraintShapesGraph = RDFDataMgr.loadGraph("shapes/constraints.ttl");
        Graph entityShapesGraph = RDFDataMgr.loadGraph(getShapesPath());
        Graph mediaTypesGraph = getMediaTypesGraph();

        Graph[] graphs = new Graph[]{
            constraintShapesGraph,
            entityShapesGraph,
            mediaTypesGraph
        };

        return ModelFactory.createModelForGraph(new MultiUnion(graphs));
    }

    private Graph getMediaTypesGraph() {
        Model model = ModelFactory
            .createDefaultModel()
            .setNsPrefixes(PrefixMapping.Standard)
            .setNsPrefix(FDK.PREFIX, FDK.NS);

        var rdfNodes = mediaTypeService
            .getMediaTypes()
            .map(mediaType -> {
                Set<Node> nodes = new HashSet<>();

                if (mediaType.getCode() != null) {
                    nodes.add(NodeFactory.createLiteral(mediaType.getCode()));
                }

                if (mediaType.getUri() != null) {
                    nodes.add(NodeFactory.createURI(mediaType.getUri()));
                }

                return Flux.fromIterable(nodes);
            })
            .flatMapIterable(Flux::toIterable)
            .map(ModelUtils::convertGraphNodeToRDFNode)
            .toStream()
            .toArray(RDFNode[]::new);

        Resource list = model.createList(rdfNodes)
            .asResource()
            .addProperty(RDF.type, RDF.List);

        ResourceUtils.renameResource(list, FDK.uri("MediaTypes"));

        return model.getGraph();
    }

}
