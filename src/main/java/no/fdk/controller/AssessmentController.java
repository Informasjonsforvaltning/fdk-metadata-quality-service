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
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@RestController
@CrossOrigin(origins = {"*"})
@RequestMapping("/assessment")
@RequiredArgsConstructor
public class AssessmentController {

    private final AssessmentService assessmentService;

    @PostMapping
    public Flux<Assessment> assessGraphNodes(
        @RequestHeader(HttpHeaders.CONTENT_TYPE) final String contentType,
        @RequestParam final String entityType,
        @RequestBody final String body
    ) {
        Lang requestBodyLang = LanguageUtils.mediaTypeToRdfLanguage(contentType);

        Graph graph = GraphUtils.stringToGraph(body, requestBodyLang);

        return assessmentService.assess(graph, EntityType.valueOfLabel(entityType));
    }

    @GetMapping("/catalog/rating")
    public Mono<Rating> getCatalogAssessmentRating(
        @RequestParam(required = false) final String catalogId,
        @RequestParam(required = false) final String catalogUri,
        @RequestParam(required = false) final String entityType
    ) {
        return assessmentService.getCatalogAssessmentRating(catalogId, catalogUri, entityType);
    }

    @GetMapping("/entity")
    public Mono<Assessment> getEntityAssessment(
        @RequestParam final String entityUri
    ) {
        return assessmentService.getEntityAssessment(entityUri);
    }

    @GetMapping("/entities")
    public Flux<Assessment> getEntitiesAssessments(
        @RequestParam final Set<String> entityUris
    ) {
        return assessmentService.getEntitiesAssessments(entityUris);
    }

}
