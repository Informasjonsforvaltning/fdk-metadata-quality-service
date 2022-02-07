package no.fdk.validation;

import no.fdk.model.ValidationResult;
import org.apache.jena.rdf.model.Model;
import reactor.core.publisher.Mono;

public interface Validator {

    boolean supports(Model model);

    Mono<ValidationResult> validate(Model model);

}
