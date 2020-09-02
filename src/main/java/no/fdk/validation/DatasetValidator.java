package no.fdk.validation;

import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCAT;
import org.springframework.stereotype.Component;

@Component
public class DatasetValidator extends EntityValidator {

    @Override
    protected Resource getResource() {
        return DCAT.Dataset;
    }

    @Override
    protected String getShapesPath() {
        return "shapes/dataset.ttl";
    }

}
