package no.fdk.validation;

import org.apache.jena.graph.Graph;
import org.apache.jena.graph.compose.MultiUnion;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.vocabulary.RDF;
import org.topbraid.shacl.validation.ValidationUtil;

public abstract class EntityValidator implements Validator {

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

        Graph[] graphs = new Graph[]{
            constraintShapesGraph,
            entityShapesGraph
        };

        return ModelFactory.createModelForGraph(new MultiUnion(graphs));
    }

}
