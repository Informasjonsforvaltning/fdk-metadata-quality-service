package no.fdk.controller;

import lombok.RequiredArgsConstructor;
import no.fdk.service.ValidationService;
import no.fdk.utils.GraphUtils;
import no.fdk.utils.LanguageUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.shacl.ValidationReport;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/validation")
@RequiredArgsConstructor
public class ValidationController {

    private final ValidationService validationService;

    @PostMapping
    private Mono<String> validateGraph(
        @RequestHeader("content-type") String contentType,
        @RequestHeader("accept") String accept,
        @RequestBody String body
    ) {
        Lang requestBodyLang = LanguageUtils.mediaTypeToRdfLanguage(contentType);
        Lang responseBodyLang = LanguageUtils.mediaTypeToRdfLanguage(accept);

        Graph bodyGraph = GraphUtils.stringToGraph(body, requestBodyLang);

        ValidationReport report = validationService.validate(bodyGraph);

        return Mono.just(GraphUtils.graphToString(report.getGraph(), responseBodyLang));
    }

}
