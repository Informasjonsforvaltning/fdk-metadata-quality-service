package no.fdk.service;

import lombok.RequiredArgsConstructor;
import no.fdk.exception.UnprocessableEntityException;
import no.fdk.validation.DatasetValidator;
import org.apache.jena.graph.Graph;
import org.apache.jena.shacl.ValidationReport;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@Service
@RequiredArgsConstructor
public class ValidationService {

    private final DatasetValidator datasetValidator;

    public Mono<ValidationReport> validate(Graph graph) {
        return Mono.defer(() ->
            Mono.justOrEmpty(graph)
                .filter(datasetValidator::supports)
                .flatMap(datasetValidator::validate)
                .switchIfEmpty(Mono.error(new UnprocessableEntityException("Could not validate data graph"))))
            .subscribeOn(Schedulers.boundedElastic());
    }

}
