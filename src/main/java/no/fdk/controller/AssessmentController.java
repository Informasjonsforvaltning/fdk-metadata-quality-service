package no.fdk.controller;

import lombok.RequiredArgsConstructor;
import no.fdk.model.Assessment;
import no.fdk.model.EntityType;
import no.fdk.service.AssessmentService;
import no.fdk.utils.GraphUtils;
import no.fdk.utils.LanguageUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/assessment")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @PostMapping
    private Flux<Assessment> assessGraphNodes(
        @RequestHeader("content-type") String contentType,
        @RequestParam EntityType entity,
        @RequestBody String body
    ) {
        Lang requestBodyLang = LanguageUtils.mediaTypeToRdfLanguage(contentType);

        Graph bodyGraph = GraphUtils.stringToGraph(body, requestBodyLang);

        return Flux.fromIterable(assessmentService.assess(bodyGraph, entity));
    }

}
