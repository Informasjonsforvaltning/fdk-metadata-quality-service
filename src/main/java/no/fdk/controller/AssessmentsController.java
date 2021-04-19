package no.fdk.controller;

import lombok.RequiredArgsConstructor;
import no.fdk.model.Assessment;
import no.fdk.model.Context;
import no.fdk.model.EntityType;
import no.fdk.service.AssessmentService;
import no.fdk.utils.GraphUtils;
import no.fdk.utils.LanguageUtils;
import org.apache.jena.graph.Graph;
import org.apache.jena.riot.Lang;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Set;

@RestController
@CrossOrigin(origins = {"*"})
@RequestMapping("/assessments")
@RequiredArgsConstructor
public class AssessmentsController {

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

    @GetMapping("/entities")
    public Mono<Page<Assessment>> listAssessments(
        @RequestParam(required = false) final Set<String> ids,
        @RequestParam(required = false) final String catalogId,
        @RequestParam(required = false) final String entityType,
        @RequestParam(required = false, defaultValue = "FDK") final Set<Context> contexts,
        @RequestParam(required = false, defaultValue = "0") final Integer page,
        @RequestParam(required = false, defaultValue = "10") final Integer size
    ) {
        return assessmentService.listAssessments(
            ids,
            catalogId,
            EntityType.valueOfLabel(entityType),
            contexts,
            PageRequest.of(page, size)
        );
    }

    @GetMapping("/entities/{id}")
    public Mono<Assessment> getAssessment(@PathVariable final String id) {
        return assessmentService.getAssessment(id);
    }

}
