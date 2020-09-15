package no.fdk.controller;

import lombok.RequiredArgsConstructor;
import no.fdk.model.Assessment;
import no.fdk.model.EntityType;
import no.fdk.model.Rating;
import no.fdk.service.AssessmentService;
import no.fdk.utils.GraphUtils;
import no.fdk.utils.LanguageUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@RestController
@RequestMapping("/assessment")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @PostMapping
    private Flux<Assessment> assessGraphNodes(
        @RequestHeader("content-type") String contentType,
        @RequestParam EntityType entityType,
        @RequestBody String body
    ) {
        Lang requestBodyLang = LanguageUtils.mediaTypeToRdfLanguage(contentType);

        Graph graph = GraphUtils.stringToGraph(body, requestBodyLang);

        return assessmentService.assess(graph, entityType);
    }

    @GetMapping("/catalog/rating")
    private Mono<Rating> getCatalogAssessmentRating(
        @RequestParam String catalogUri,
        @RequestParam EntityType entityType
    ) {
        return assessmentService.getCatalogAssessmentRating(catalogUri, entityType);
    }

    @GetMapping("/entity")
    private Mono<Assessment> getEntityAssessment(
        @RequestParam String entityUri
    ) {
        return assessmentService.getEntityAssessment(entityUri);
    }

    @GetMapping("/entities")
    private Flux<Assessment> getEntitiesAssessments(
        @RequestParam Set<String> entityUris
    ) {
        return assessmentService.getEntitiesAssessments(entityUris);
    }

}
