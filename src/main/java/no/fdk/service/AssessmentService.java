package no.fdk.service;

import com.mongodb.BasicDBList;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fdk.exception.BadRequestException;
import no.fdk.exception.NotFoundException;
import no.fdk.model.*;
import no.fdk.repository.AssessmentRepository;
import no.fdk.utils.AssessmentUtils;
import no.fdk.utils.PaginationUtils;
import org.apache.jena.graph.Graph;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.ReactiveFluentMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
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
        log.info("Starting validation of data graph for entity type: {}", entityType);

        return validationService.validate(graph)
            .doOnNext(report -> log.info("Successfully created validation report for entity type: {}", entityType))
            .flatMapMany(report -> AssessmentUtils.extractEntityResourcePairsFromGraph(graph, entityType, report))
            .delayElements(Duration.ofMillis(150))
            .map(AssessmentUtils::generateAssessmentForEntity)
            .doOnComplete(() -> log.info("Successfully created quality assessments for entity type: {}", entityType));
    }

    public Mono<Rating> getCatalogAssessmentRating(String catalogId, String catalogUri, String entityType, Collection<Context> contexts) {
        if (catalogId == null && catalogUri == null) {
            throw new BadRequestException("One of [catalogId, catalogUri] must be provided");
        }

        return (catalogId != null
            ? assessmentRepository.findAllByEntityCatalogIdAndEntityTypeAndEntityContextsIn(catalogId, EntityType.valueOfLabel(entityType), contexts)
            : assessmentRepository.findAllByEntityCatalogUriAndEntityTypeAndEntityContextsIn(catalogUri, EntityType.valueOfLabel(entityType), contexts))
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

    public Mono<Assessment> getAssessment(String id) {
        return assessmentRepository
            .findById(id)
            .switchIfEmpty(Mono.error(new NotFoundException(format("Could not find an assessment with ID: %s", id))));
    }

    @Deprecated
    public Flux<Assessment> getAssessments(Set<String> entityUris) {
        return assessmentRepository.findAllByEntityUriIn(entityUris);
    }

    public Mono<Page<Assessment>> listAssessments(
        Set<String> ids,
        String catalogId,
        EntityType entityType,
        Set<Context> contexts,
        Pageable pageable
    ) {
        if (ids != null && !ids.isEmpty()) {
            Flux<Assessment> assessments = assessmentRepository.findAllById(ids);

            return Mono.zip(assessments.collectList(), assessments.count()).map(PaginationUtils::toPage);
        }

        Entity entity = Entity
            .builder()
            .catalog(Catalog.builder().id(catalogId).build())
            .type(entityType)
            .contexts(contexts)
            .build();
        Assessment assessment = Assessment
            .builder()
            .entity(entity)
            .build();

        ExampleMatcher matcher = ExampleMatcher
            .matching()
            .withIgnoreNullValues()
            .withMatcher("entity.contexts", match -> match.transform(source -> source.map(o -> ((BasicDBList) o).iterator().next())).exact());
        Example<Assessment> example = Example.of(assessment, matcher);

        Flux<Assessment> assessments = assessmentRepository.findAll(example);
        Flux<Assessment> assessmentsSubset = assessments
            .skip(pageable.getOffset())
            .take(pageable.getPageSize());

        return Mono.zip(assessmentsSubset.collectList(), Mono.justOrEmpty(pageable), assessments.count())
            .map(PaginationUtils::toPage);
    }

    public Mono<Page<Assessment>> listAssessments(String catalogId, EntityType entityType, Collection<Context> contexts, Pageable pageable) {
        Mono<Long> count = assessmentRepository.countByEntityCatalogIdAndEntityTypeAndEntityContextsIn(catalogId, entityType, contexts);
        Mono<List<Assessment>> assessments = assessmentRepository.findAllByEntityCatalogIdAndEntityTypeAndEntityContextsIn(catalogId, entityType, contexts, pageable).collectList();

        return Mono.zip(assessments, Mono.justOrEmpty(pageable), count).map(PaginationUtils::toPage);
    }

    public void upsertAssessment(Assessment assessment) {
        reactiveFluentMongoOperations
            .update(Assessment.class)
            .matching(Criteria.where(UNDERSCORE_ID).is(assessment.getId()))
            .replaceWith(assessment)
            .withOptions(FindAndReplaceOptions.options().returnNew().upsert())
            .findAndReplace()
            .doOnError(Throwable::printStackTrace)
            .subscribe();
    }

}
