package no.fdk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fdk.model.ValidationResult;
import no.fdk.validation.DatasetValidator;
import org.apache.jena.rdf.model.Model;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
@Slf4j
public class ValidationService {

    private final DatasetValidator datasetValidator;

    public Mono<ValidationResult> validate(Model model) {
        return Mono.defer(() ->
            Mono.justOrEmpty(model)
                .filter(datasetValidator::supports)
                .flatMap(datasetValidator::validate)
                )
            .subscribeOn(Schedulers.boundedElastic());
    }
}
