package no.fdk.service;

import lombok.RequiredArgsConstructor;
import no.fdk.exception.UnprocessableEntityException;
import no.fdk.validation.DatasetValidator;
import org.apache.jena.graph.Graph;
import org.apache.jena.shacl.ValidationReport;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final DatasetValidator datasetValidator;

    public ValidationReport validate(Graph graph) {
        if (datasetValidator.supports(graph)) {
            return datasetValidator.validate(graph);
        }

        throw new UnprocessableEntityException("Could not validate data graph");
    }

}
