package no.fdk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fdk.exception.BadRequestException;
import no.fdk.exception.NotFoundException;
import no.fdk.model.*;
import no.fdk.repository.AssessmentRepository;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.util.URIref;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.springframework.data.mongodb.core.FindAndReplaceOptions;
import org.springframework.data.mongodb.core.ReactiveFluentMongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

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
        Collection<Assessment> assessments = new ArrayList<>();
        ValidationReport report = validationService.validate(graph);

        extractEntitiesFromGraph(graph, entityType)
            .forEach(entity -> assessments.add(generateAssessmentForEntity(entity, report)));

        return Flux.fromIterable(assessments);
    }

    public Mono<Rating> getCatalogAssessmentRating(String catalogId, String catalogUri, EntityType entityType) {
        if (catalogId == null && catalogUri == null) {
            throw new BadRequestException("One of [catalogId, catalogUri] must be provided");
        }

        return (catalogId != null
            ? assessmentRepository.findAllByEntityCatalogIdAndEntityType(catalogId, entityType)
            : assessmentRepository.findAllByEntityCatalogUriAndEntityType(catalogUri, entityType))
            .map(Assessment::getRating)
            .reduce((current, previous) ->
                Rating
                    .builder()
                    .score(previous.getScore() + current.getScore())
                    .maxScore(previous.getMaxScore() + current.getMaxScore())
                    .category(determineRatingCategory(previous.getScore() + current.getScore(), previous.getMaxScore() + current.getMaxScore()))
                    .build())
            .switchIfEmpty(Mono.error(new NotFoundException(format("Could not find any entries with catalog URI: %s and entity type: %s", catalogUri, entityType))));
    }

    public Mono<Assessment> getEntityAssessment(String entityUri) {
        return assessmentRepository
            .findById(entityUri)
            .switchIfEmpty(Mono.error(new NotFoundException(format("Could not find any entries with entity URI: %s", entityUri))));
    }

    public Flux<Assessment> getEntitiesAssessments(Set<String> entityUris) {
        return assessmentRepository.findAllByEntityUriIn(entityUris);
    }

    public Flux<Assessment> upsertAssessments(Flux<Assessment> assessments) {
        return assessments
            .flatMap(assessment -> reactiveFluentMongoOperations
                .update(Assessment.class)
                .matching(Criteria.where(UNDERSCORE_ID).is(assessment.getId()))
                .replaceWith(assessment)
                .withOptions(FindAndReplaceOptions.options().returnNew().upsert())
                .findAndReplace()
            );
    }

    private Collection<Dimension> buildDimensions(Collection<IndicatorType> violations) {
        List<Indicator> accessibilityIndicators = List.of(
            Indicator
                .builder()
                .type(IndicatorType.access_url)
                .weight(10)
                .conforms(!violations.contains(IndicatorType.access_url))
                .build()
        );
        List<Indicator> findabilityIndicators = List.of(
            Indicator
                .builder()
                .type(IndicatorType.keyword)
                .weight(10)
                .conforms(!violations.contains(IndicatorType.keyword))
                .build(),
            Indicator
                .builder()
                .type(IndicatorType.subject)
                .weight(20)
                .conforms(!violations.contains(IndicatorType.subject))
                .build()
        );

        return List.of(
            Dimension
                .builder()
                .type(DimensionType.accessibility)
                .indicators(accessibilityIndicators)
                .rating(buildRating(accessibilityIndicators))
                .build(),
            Dimension
                .builder()
                .type(DimensionType.findability)
                .indicators(findabilityIndicators)
                .rating(buildRating(findabilityIndicators))
                .build()
        );
    }

    private Rating buildRating(Collection<Indicator> indicators) {
        Integer score = indicators
            .stream()
            .filter(Indicator::getConforms)
            .mapToInt(Indicator::getWeight)
            .sum();
        Integer maxScore = indicators
            .stream()
            .mapToInt(Indicator::getWeight)
            .sum();

        return Rating
            .builder()
            .score(score)
            .maxScore(maxScore)
            .category(determineRatingCategory(score, maxScore))
            .build();
    }

    private RatingCategory determineRatingCategory(Integer score, Integer maxScore) {
        double ratio = score.doubleValue() / maxScore.doubleValue();

        if (ratio >= 0.75) {
            return RatingCategory.excellent;
        } else if (ratio >= 0.5 && ratio < 0.75) {
            return RatingCategory.good;
        } else if (ratio >= 0.25 && ratio < 0.50) {
            return RatingCategory.sufficient;
        }

        return RatingCategory.poor;
    }

    private Collection<Entity> extractEntitiesFromGraph(Graph graph, EntityType entityType) {
        Collection<Entity> entities = new ArrayList<>();
        Model model = ModelFactory.createModelForGraph(graph);

        if (entityType == EntityType.dataset) {
            model
                .listResourcesWithProperty(RDF.type, DCAT.Dataset)
                .toList()
                .forEach(r -> {
                    Entity entity = Entity
                        .builder()
                        .uri(r.getURI())
                        .type(EntityType.dataset)
                        .catalog(extractCatalogFromModel(model, r.getURI()))
                        .build();

                    entities.add(entity);
                });
        }

        return entities;
    }

    private Assessment generateAssessmentForEntity(Entity entity, ValidationReport report) {
        Collection<IndicatorType> violations = new HashSet<>();

        if (!report.conforms()) {
            Collection<ReportEntry> reportEntries = report.getEntries();

            reportEntries
                .stream()
                .filter(entry -> entry.focusNode().hasURI(entity.getUri()))
                .forEach(entry -> violations.addAll(getViolations(entry, reportEntries)));
        }

        Collection<Dimension> dimensions = buildDimensions(violations);
        Collection<Indicator> indicators = dimensions
            .stream()
            .map(Dimension::getIndicators)
            .collect(ArrayList::new, List::addAll, List::addAll);

        return Assessment
            .builder()
            .id(entity.getUri())
            .entity(entity)
            .dimensions(dimensions)
            .rating(buildRating(indicators))
            .build();
    }

    private Collection<IndicatorType> getViolations(ReportEntry entry, Collection<ReportEntry> reportEntries) {
        Collection<IndicatorType> violations = new HashSet<>();

        if (entry.value() != null) {
            reportEntries
                .stream()
                .filter(e -> {
                    if (e.focusNode().isURI() && entry.value().isURI()) {
                        return e.focusNode().getURI().equals(entry.value().getURI());
                    }

                    if (e.focusNode().isBlank() && entry.value().isBlank()) {
                        return e.focusNode().getBlankNodeId().equals(entry.value().getBlankNodeId());
                    }

                    return false;
                })
                .findAny()
                .ifPresent(relatedEntry -> violations.addAll(getViolations(relatedEntry, reportEntries)));
        } else {
            String path = entry.resultPath().toString().replaceAll("^<|>$", "");

            if (path.equals(DCAT.keyword.getURI())) {
                violations.add(IndicatorType.keyword);
            }

            if (path.equals(DCAT.accessURL.getURI()) || path.equals(DCAT.endpointURL.getURI())) {
                violations.add(IndicatorType.access_url);
            }

            if (path.equals(DCTerms.subject.getURI())) {
                violations.add(IndicatorType.subject);
            }
        }

        return violations;
    }

    private Catalog extractCatalogFromModel(Model model, String uri) {
        Catalog.CatalogBuilder catalogBuilder = Catalog.builder();

        ResIterator catalogIterator = model.listResourcesWithProperty(DCAT.dataset, ResourceFactory.createResource(URIref.encode(uri)));

        if (catalogIterator.hasNext()) {
            Resource catalogResource = catalogIterator.nextResource();

            catalogBuilder
                .id(extractPublisherIdFromCatalogResource(catalogResource))
                .uri(catalogResource.getURI());
        }

        return catalogBuilder.build();
    }

    private String extractPublisherIdFromCatalogResource(Resource resource) {
        Resource publisherResource = resource.hasProperty(DCTerms.publisher)
            ? resource.getProperty(DCTerms.publisher).getResource()
            : null;

        return publisherResource != null && publisherResource.hasProperty(DCTerms.identifier)
            ? publisherResource.getProperty(DCTerms.identifier).getString()
            : null;
    }

}
