package no.fdk.validation;

import org.apache.jena.graph.Graph;
import org.apache.jena.shacl.ValidationReport;

public interface Validator {
    boolean supports(Graph graph);

    ValidationReport validate(Graph graph);
}
