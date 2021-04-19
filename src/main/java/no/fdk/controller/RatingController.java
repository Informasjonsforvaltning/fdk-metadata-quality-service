package no.fdk.controller;

import lombok.RequiredArgsConstructor;
import no.fdk.model.Context;
import no.fdk.model.Rating;
import no.fdk.service.AssessmentService;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.Set;

@RestController
@CrossOrigin(origins = {"*"})
@RequestMapping("/rating")
@RequiredArgsConstructor
public class RatingController {

    private final AssessmentService assessmentService;

    @GetMapping("/catalog")
    public Mono<Rating> getCatalogAssessmentRating(
        @RequestParam(required = false) final String catalogId,
        @RequestParam(required = false) final String catalogUri,
        @RequestParam(required = false) final String entityType,
        @RequestParam(required = false, defaultValue = "FDK") final Set<Context> contexts
    ) {
        return assessmentService.getCatalogAssessmentRating(catalogId, catalogUri, entityType, contexts);
    }

}
