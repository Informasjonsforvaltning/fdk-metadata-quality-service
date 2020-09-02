package no.fdk.service;

import lombok.RequiredArgsConstructor;
import no.fdk.model.*;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.ModelFactory;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class AssessmentService {

    private final ValidationService validationService;

    public Collection<Assessment> assess(Graph graph, EntityType entity) {
        Collection<Assessment> assessments = new ArrayList<>();
        ValidationReport report = validationService.validate(graph);

        extractEntitiesFromGraph(graph, entity)
            .forEach((id, uri) -> assessments.add(generateAssessmentForEntity(id, uri, entity, report)));

        return assessments;
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

    private Map<String, String> extractEntitiesFromGraph(Graph graph, EntityType entity) {
        Map<String, String> entities = new HashMap<>();
        Model model = ModelFactory.createModelForGraph(graph);

        if (entity == EntityType.dataset) {
            model
                .listResourcesWithProperty(RDF.type, DCAT.Dataset)
                .toList()
                .forEach(r -> {
                    if (r.hasProperty(DCTerms.identifier)) {
                        String resourceUri = r.getURI();
                        String identifier = r.getProperty(DCTerms.identifier).getString();
                        entities.put(identifier, resourceUri);
                    }
                });
        }

        return entities;
    }

    private Assessment generateAssessmentForEntity(String id, String uri, EntityType entity, ValidationReport report) {
        Collection<IndicatorType> violations = new HashSet<>();

        if (!report.conforms()) {
            Collection<ReportEntry> reportEntries = report.getEntries();

            reportEntries
                .stream()
                .filter(entry -> entry.focusNode().hasURI(uri))
                .forEach(entry -> violations.addAll(getViolations(entry, reportEntries)));
        }

        Collection<Dimension> dimensions = buildDimensions(violations);
        Collection<Indicator> indicators = dimensions
            .stream()
            .map(Dimension::getIndicators)
            .collect(ArrayList::new, List::addAll, List::addAll);

        return Assessment
            .builder()
            .enityId(id)
            .entityType(entity)
            .dimensions(dimensions)
            .rating(buildRating(indicators))
            .build();
    }

    private Collection<IndicatorType> getViolations(ReportEntry entry, Collection<ReportEntry> reportEntries) {
        Collection<IndicatorType> violations = new HashSet<>();

        if (entry.value() != null) {
            reportEntries
                .stream()
                .filter(e -> e.focusNode().getURI().equals(entry.value().getURI()))
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

}
