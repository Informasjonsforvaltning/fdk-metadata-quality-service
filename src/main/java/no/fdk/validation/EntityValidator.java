package no.fdk.validation;

import org.apache.jena.graph.Graph;
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
        Graph shapesGraph = RDFDataMgr.loadGraph(getShapesPath());

        Model model = ModelFactory.createModelForGraph(graph);
        Model shapesModel = ModelFactory.createModelForGraph(shapesGraph);

        Resource report = ValidationUtil.validateModel(model, shapesModel, true);

        return ValidationReport.fromModel(report.getModel());
    }

    protected abstract Resource getResource();

    protected abstract String getShapesPath();

}
