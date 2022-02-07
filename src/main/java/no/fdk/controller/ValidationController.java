package no.fdk.controller;

import lombok.RequiredArgsConstructor;
import no.fdk.model.ValidationResult;
import no.fdk.service.ValidationService;
import no.fdk.utils.GraphUtils;
import no.fdk.utils.LanguageUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.apache.jena.shacl.ValidationReport;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import static org.apache.jena.rdf.model.ModelFactory.createModelForGraph;

@RestController
@RequestMapping("/validation")
@RequiredArgsConstructor
public class ValidationController {

    private final ValidationService validationService;

    @PostMapping
    public Mono<String> validateGraph(
        @RequestHeader(HttpHeaders.CONTENT_TYPE) final String contentType,
        @RequestHeader(HttpHeaders.ACCEPT) final String accept,
        @RequestBody final String body
    ) {
        Lang requestBodyLang = LanguageUtils.mediaTypeToRdfLanguage(contentType);
        Lang responseBodyLang = LanguageUtils.mediaTypeToRdfLanguage(accept);

        Graph bodyGraph = GraphUtils.stringToGraph(body, requestBodyLang);

        return validationService.validate(createModelForGraph(bodyGraph))
            .map(ValidationResult::getValidationReport)
            .map(ValidationReport::getGraph)
            .map(graph -> GraphUtils.graphToString(graph, responseBodyLang));
    }

}
