package no.fdk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fdk.exception.BadRequestException;
import no.fdk.exception.NotFoundException;
import no.fdk.model.Assessment;
import no.fdk.model.EntityType;
import no.fdk.model.Rating;
import no.fdk.repository.AssessmentRepository;
import no.fdk.utils.AssessmentUtils;
import org.apache.jena.graph.Graph;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.ReactiveFluentMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Set;

import static java.lang.String.format;
import static org.springframework.data.mongodb.core.aggregation.Fields.UNDERSCORE_ID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AssessmentService {

    private final ValidationService validationService;
    private final AssessmentRepository assessmentRepository;
    private final ReactiveFluentMongoOperations reactiveFluentMongoOperations;

    public Flux<Assessment> assess(Graph graph, EntityType entityType) {
        return AssessmentUtils.extractEntityResourcePairsFromGraph(graph, entityType, validationService.validate(graph))
            .map(AssessmentUtils::generateAssessmentForEntity);
    }

    public Mono<Rating> getCatalogAssessmentRating(String catalogId, String catalogUri, EntityType entityType) {
        if (catalogId == null && catalogUri == null) {
            throw new BadRequestException("One of [catalogId, catalogUri] must be provided");
        }

        return (catalogId != null
            ? assessmentRepository.findAllByEntityCatalogIdAndEntityType(catalogId, entityType)
            : assessmentRepository.findAllByEntityCatalogUriAndEntityType(catalogUri, entityType))
            .reduce(
                Rating
                    .builder()
                    .score(0)
                    .maxScore(0)
                    .satisfiedCriteria(0)
                    .totalCriteria(0)
                    .dimensionsRating(Collections.emptyMap())
                    .build(),
                AssessmentUtils::buildAggregatedRating
            )
            .doOnError(Throwable::printStackTrace)
            .switchIfEmpty(Mono.error(new NotFoundException(format("Could not find any entries with catalog ID: %s or catalog URI: %s and entity type: %s", catalogId, catalogUri, entityType))));
    }

    public Mono<Assessment> getEntityAssessment(String entityUri) {
        return assessmentRepository
            .findById(entityUri)
            .switchIfEmpty(Mono.error(new NotFoundException(format("Could not find any entries with entity URI: %s", entityUri))));
    }

    public Flux<Assessment> getEntitiesAssessments(Set<String> entityUris) {
        return assessmentRepository.findAllByEntityUriIn(entityUris);
    }

    public void upsertAssessment(Assessment assessment) {
        reactiveFluentMongoOperations
            .update(Assessment.class)
            .matching(Criteria.where(UNDERSCORE_ID).is(assessment.getId()))
            .replaceWith(assessment)
            .withOptions(FindAndReplaceOptions.options().returnNew().upsert())
            .findAndReplace();
    }

}
