package no.fdk.validation;

import org.apache.jena.graph.Graph;
import org.apache.jena.shacl.ValidationReport;
import reactor.core.publisher.Mono;

public interface Validator {

    boolean supports(Graph graph);

    Mono<ValidationReport> validate(Graph graph);

}
