package no.fdk.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import no.fdk.model.*;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.jena.graph.Graph;
import org.apache.jena.graph.Node;
import org.apache.jena.rdf.model.*;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.shacl.validation.ReportEntry;
import org.apache.jena.sparql.path.Path;
import org.apache.jena.sparql.path.PathFactory;
import org.apache.jena.sparql.vocabulary.FOAF;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.DCTerms;
import org.apache.jena.vocabulary.RDF;
import org.apache.jena.vocabulary.VCARD4;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class AssessmentUtils {

    public static Rating buildAggregatedRating(Rating rating, Assessment assessment) {
        return Rating
            .builder()
            .score(rating.getScore() + assessment.getRating().getScore())
            .maxScore(rating.getMaxScore() + assessment.getRating().getMaxScore())
            .satisfiedCriteria(rating.getSatisfiedCriteria() + assessment.getRating().getSatisfiedCriteria())
            .totalCriteria(rating.getTotalCriteria() + assessment.getRating().getTotalCriteria())
            .category(determineRatingCategory(rating.getScore() + assessment.getRating().getScore(), rating.getMaxScore() + assessment.getRating().getMaxScore()))
            .dimensionsRating(buildAggregateDimensionsRating(rating.getDimensionsRating(), assessment.getDimensions()))
            .build();
    }

    public static Assessment generateAssessmentForEntity(Triple<Entity, Resource, Collection<ReportEntry>> triple) {
        Collection<IndicatorType> violations = triple.getRight()
            .stream()
            .map(entry -> AssessmentUtils.findViolations(entry, triple))
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        Collection<Dimension> dimensions = buildDimensions(violations);
        Collection<Indicator> indicators = dimensions
            .stream()
            .map(Dimension::getIndicators)
            .flatMap(Collection::stream)
            .collect(Collectors.toSet());

        return Assessment
            .builder()
            .id(triple.getLeft().getId())
            .entity(triple.getLeft())
            .dimensions(dimensions)
            .rating(buildRating(indicators))
            .build();
    }

    public static Flux<Triple<Entity, Resource, Collection<ReportEntry>>> extractEntityResourcePairsFromGraph(Graph graph, EntityType entityType, ValidationReport report) {
        Model model = ModelFactory.createModelForGraph(graph);

        if (entityType == EntityType.DATASET) {
            return extractDatasetEntitiesFromModel(model, report);
        }

        return Flux.empty();
    }

    private static boolean isTitlePath(Path path) {
        return path != null && path.equalTo(PathFactory.pathLink(DCTerms.title.asNode()), null);
    }

    private static boolean isDescriptionPath(Path path) {
        return path != null && path.equalTo(PathFactory.pathLink(DCTerms.description.asNode()), null);
    }

    private static boolean isDistributionPath(Path path) {
        return path != null && path.equalTo(PathFactory.pathLink(DCAT.distribution.asNode()), null);
    }

    private static boolean isAccessUrlOrAccessServiceEndpointUrlPath(Path path) {
        Path accessUrlOrAccessServiceEndpointUrlPath = PathFactory.pathAlt(PathFactory.pathLink(DCAT.accessURL.asNode()), PathFactory.pathSeq(PathFactory.pathLink(DCAT.accessService.asNode()), PathFactory.pathLink(DCAT.endpointURL.asNode())));

        return path != null && path.equalTo(accessUrlOrAccessServiceEndpointUrlPath, null);
    }

    private static boolean isDistributionFormatOrMediaTypePath(Path path) {
        return path != null && path.equalTo(PathFactory.pathAlt(PathFactory.pathLink(DCTerms.format.asNode()), PathFactory.pathLink(DCAT.mediaType.asNode())), null);
    }

    private static boolean isLicensePath(Path path) {
        return path != null && path.equalTo(PathFactory.pathLink(DCTerms.license.asNode()), null);
    }

    private static boolean isKeywordPath(Path path) {
        return path != null && path.equalTo(PathFactory.pathLink(DCAT.keyword.asNode()), null);
    }

    private static boolean isSubjectPath(Path path) {
        return path != null && path.equalTo(PathFactory.pathLink(DCTerms.subject.asNode()), null);
    }

    private static boolean isSpatialPath(Path path) {
        return path != null && path.equalTo(PathFactory.pathLink(DCTerms.spatial.asNode()), null);
    }

    private static boolean isContactPointPath(Path path) {
        return path != null && path.equalTo(PathFactory.pathLink(DCAT.contactPoint.asNode()), null);
    }

    private static boolean isContactPointPropertyPath(Path path) {
        Path organizationNamePath = PathFactory.pathLink(VCARD4.organization_name.asNode());
        Path organizationUnitPath = PathFactory.pathLink(VCARD4.organization_unit.asNode());
        Path hasOrganizationNamePath = PathFactory.pathLink(VCARD4.hasOrganizationName.asNode());
        Path hasOrganizationUnitPath = PathFactory.pathLink(VCARD4.hasOrganizationUnit.asNode());
        Path hasURLPath = PathFactory.pathLink(VCARD4.hasURL.asNode());
        Path hasEmailPath = PathFactory.pathLink(VCARD4.hasEmail.asNode());
        Path hasTelephonePath = PathFactory.pathLink(VCARD4.hasTelephone.asNode());

        return path != null &&
            (path.equalTo(organizationNamePath, null)
                || path.equalTo(organizationUnitPath, null)
                || path.equalTo(hasOrganizationNamePath, null)
                || path.equalTo(hasOrganizationUnitPath, null)
                || path.equalTo(hasURLPath, null)
                || path.equalTo(hasEmailPath, null)
                || path.equalTo(hasTelephonePath, null));
    }

    private static boolean hasFormatOrMediaType(Resource resource) {
        return resource.hasProperty(DCTerms.format) || resource.hasProperty(DCAT.mediaType);
    }

    private static boolean hasLicense(Resource resource) {
        return resource.hasProperty(DCTerms.license);
    }

    private static boolean hasAccessServiceEndpointUrl(Resource resource) {
        return resource
            .listProperties(DCAT.accessService)
            .toList()
            .stream()
            .map(Statement::getResource)
            .anyMatch(r -> r.hasProperty(DCAT.endpointURL));
    }

    private static boolean hasAccessUrlOrAccessServiceEndpointUrl(Resource resource) {
        return resource.hasProperty(DCAT.accessURL) || hasAccessServiceEndpointUrl(resource);
    }

    private static boolean hasControlledVocabularyViolations(Collection<Resource> distributions, Collection<ReportEntry> otherReportEntries) {
        boolean isMissingFormatOrMediaTypeProperties = distributions.stream().noneMatch(AssessmentUtils::hasFormatOrMediaType);

        long formatOrMediaTypeViolationsCount = otherReportEntries
            .stream()
            .map(ReportEntry::resultPath)
            .filter(AssessmentUtils::isDistributionFormatOrMediaTypePath)
            .count();

        return isMissingFormatOrMediaTypeProperties || formatOrMediaTypeViolationsCount >= distributions.size();
    }

    private static boolean hasLicenseInformationViolations(Collection<Resource> distributions, Collection<ReportEntry> otherReportEntries) {
        boolean isMissingLicenseProperties = distributions.stream().noneMatch(AssessmentUtils::hasLicense);

        long licenseViolationsCount = otherReportEntries
            .stream()
            .map(ReportEntry::resultPath)
            .filter(AssessmentUtils::isLicensePath)
            .count();

        return isMissingLicenseProperties || licenseViolationsCount >= distributions.size();
    }

    private static boolean hasDistributableDataViolations(Collection<Resource> distributions, Collection<ReportEntry> otherReportEntries) {
        boolean isMissingAccessUrlOrAccessServiceEndpointUrl = distributions.stream().noneMatch(AssessmentUtils::hasAccessUrlOrAccessServiceEndpointUrl);

        long formatOrMediaTypeViolationsCount = otherReportEntries
            .stream()
            .map(ReportEntry::resultPath)
            .filter(AssessmentUtils::isAccessUrlOrAccessServiceEndpointUrlPath)
            .count();

        return isMissingAccessUrlOrAccessServiceEndpointUrl || formatOrMediaTypeViolationsCount >= distributions.size();
    }

    private static String extractPublisherIdFromCatalogResource(Resource catalogResource) {
        Resource publisherResource = catalogResource != null ? catalogResource.getPropertyResourceValue(DCTerms.publisher) : null;

        return publisherResource != null && publisherResource.hasProperty(DCTerms.identifier)
            ? publisherResource.getProperty(DCTerms.identifier).getString()
            : null;
    }

    private static String extractPublisherIdFromPublisherResource(Resource publisherResource) {
        String identifier = publisherResource != null && publisherResource.hasProperty(DCTerms.identifier)
            ? publisherResource.getProperty(DCTerms.identifier).getString()
            : null;

        return identifier != null && Pattern.matches("\\d{9}", identifier)
            ? identifier
            : null;
    }

    private static Catalog extractCatalogFromModel(Model model, Resource datasetResource) {
        String catalogId = null;
        String catalogUri = null;

        if (datasetResource.hasProperty(DCTerms.publisher)) {
            catalogId = extractPublisherIdFromPublisherResource(datasetResource.getPropertyResourceValue(DCTerms.publisher));
        }

        if (catalogId == null) {
            Resource catalogResource = model.listResourcesWithProperty(DCAT.dataset, datasetResource)
                .toList()
                .stream()
                .findFirst()
                .orElse(null);

            catalogId = extractPublisherIdFromCatalogResource(catalogResource);
            catalogUri = catalogResource != null ? catalogResource.getURI() : null;
        }

        return Catalog
            .builder()
            .id(catalogId)
            .uri(catalogUri)
            .build();
    }

    private static String extractFdkIdFromResource(Resource resource) {
        return resource
            .getModel()
            .listSubjectsWithProperty(FOAF.primaryTopic, resource)
            .toList()
            .stream()
            .filter(r -> r.hasProperty(RDF.type, DCAT.CatalogRecord))
            .findFirst()
            .map(r -> r.getProperty(DCTerms.identifier))
            .map(Statement::getObject)
            .map(RDFNode::asLiteral)
            .map(Literal::getString)
            .orElse(null);
    }

    private static Map<String, String> extractTitleFromResource(Resource resource, Property property) {
        return resource
            .listProperties(property)
            .toList()
            .stream()
            .map(Statement::getLiteral)
            .filter(Objects::nonNull)
            .collect(Collectors.toMap(Literal::getLanguage, Literal::getLexicalForm));
    }

    private static Collection<Context> extractContextsFromResource(Resource resource) {
        return Set.of(Context.FDK);
    }

    private static RatingCategory determineRatingCategory(Integer score, Integer maxScore) {
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

    private static Rating buildRating(Collection<Indicator> indicators) {
        Integer score = indicators
            .stream()
            .filter(Indicator::getConforms)
            .mapToInt(Indicator::getWeight)
            .sum();
        Integer maxScore = indicators
            .stream()
            .mapToInt(Indicator::getWeight)
            .sum();
        long satisfiedCriteria = indicators
            .stream()
            .filter(Indicator::getConforms)
            .count();

        return Rating
            .builder()
            .score(score)
            .maxScore(maxScore)
            .satisfiedCriteria(Math.toIntExact(satisfiedCriteria))
            .totalCriteria(indicators.size())
            .category(determineRatingCategory(score, maxScore))
            .build();
    }

    private static Collection<Dimension> buildDimensions(Collection<IndicatorType> violations) {
        List<Indicator> accessibilityIndicators = List.of(
            Indicator
                .builder()
                .type(IndicatorType.distributableData)
                .weight(100)
                .conforms(!violations.contains(IndicatorType.distributableData))
                .build()
        );
        List<Indicator> findabilityIndicators = List.of(
            Indicator
                .builder()
                .type(IndicatorType.keywordUsage)
                .weight(30)
                .conforms(!violations.contains(IndicatorType.keywordUsage))
                .build(),
            Indicator
                .builder()
                .type(IndicatorType.subjectUsage)
                .weight(60)
                .conforms(!violations.contains(IndicatorType.subjectUsage))
                .build(),
            Indicator
                .builder()
                .type(IndicatorType.geoSearch)
                .weight(10)
                .conforms(!violations.contains(IndicatorType.geoSearch))
                .build()
        );
        List<Indicator> interoperabilityIndicators = List.of(
            Indicator
                .builder()
                .type(IndicatorType.controlledVocabularyUsage)
                .weight(100)
                .conforms(!violations.contains(IndicatorType.controlledVocabularyUsage))
                .build()
        );
        List<Indicator> readabilityIndicators = List.of(
            Indicator
                .builder()
                .type(IndicatorType.title)
                .weight(15)
                .conforms(!violations.contains(IndicatorType.title))
                .build(),
            Indicator
                .builder()
                .type(IndicatorType.titleNoOrgName)
                .weight(10)
                .conforms(!violations.contains(IndicatorType.titleNoOrgName))
                .build(),
            Indicator
                .builder()
                .type(IndicatorType.description)
                .weight(10)
                .conforms(!violations.contains(IndicatorType.description))
                .build(),
            Indicator
                .builder()
                .type(IndicatorType.descriptionWithoutTitle)
                .weight(5)
                .conforms(!violations.contains(IndicatorType.descriptionWithoutTitle))
                .build()
        );
        List<Indicator> reusabilityIndicators = List.of(
            Indicator
                .builder()
                .type(IndicatorType.licenseInformation)
                .weight(60)
                .conforms(!violations.contains(IndicatorType.licenseInformation))
                .build(),
            Indicator
                .builder()
                .type(IndicatorType.contactPoint)
                .weight(40)
                .conforms(!violations.contains(IndicatorType.contactPoint))
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
                .build(),
            Dimension
                .builder()
                .type(DimensionType.interoperability)
                .indicators(interoperabilityIndicators)
                .rating(buildRating(interoperabilityIndicators))
                .build(),
            Dimension
                .builder()
                .type(DimensionType.readability)
                .indicators(readabilityIndicators)
                .rating(buildRating(readabilityIndicators))
                .build(),
            Dimension
                .builder()
                .type(DimensionType.reusability)
                .indicators(reusabilityIndicators)
                .rating(buildRating(reusabilityIndicators))
                .build()
        );
    }

    private static Map<DimensionType, Rating> buildAggregateDimensionsRating(
        Map<DimensionType, Rating> previousDimensionsRating,
        Collection<Dimension> dimensions
    ) {
        return dimensions
            .stream()
            .collect(Collectors.toMap(Dimension::getType, dimension -> {
                Rating currentRating = buildRating(dimension.getIndicators());
                Rating previousRating = previousDimensionsRating.get(dimension.getType());

                return Rating
                    .builder()
                    .score((previousRating != null ? previousRating.getScore() : 0) + currentRating.getScore())
                    .maxScore((previousRating != null ? previousRating.getMaxScore() : 0) + currentRating.getMaxScore())
                    .build();
            }));
    }

    private static boolean hasSameUri(ReportEntry entry, Entity entity) {
        return entry.focusNode().hasURI(entity.getUri());
    }

    private static boolean isNodeRelatedToResource(Resource parent, Node child) {
        return parent
            .listProperties()
            .toList()
            .stream()
            .map(Statement::getObject)
            .filter(node -> !(node.isResource() && EntityType.resources().contains(node.asResource().getPropertyResourceValue(RDF.type))))
            .map(RDFNode::asNode)
            .anyMatch(node -> node.equals(child));
    }

    private static boolean isReportEntryRelatedToEntityResource(ReportEntry entry, Entity entity, Resource entityResource) {
        return hasSameUri(entry, entity) || isNodeRelatedToResource(entityResource, entry.focusNode());
    }

    private static Collection<IndicatorType> findViolations(ReportEntry reportEntry, Triple<Entity, Resource, Collection<ReportEntry>> triple) {
        Resource entityResource = triple.getMiddle();
        Collection<ReportEntry> otherReportEntries = triple
            .getRight()
            .stream()
            .filter(Predicate.not(reportEntry::same))
            .collect(Collectors.toSet());

        Collection<IndicatorType> violations = new HashSet<>();

        Path path = reportEntry.resultPath();
        Collection<String> messages = reportEntry
            .messages()
            .stream()
            .map(Node::getLiteralValue)
            .map(Object::toString)
            .collect(Collectors.toSet());

        Collection<Resource> distributions = entityResource
            .listProperties(DCAT.distribution)
            .toList().stream()
            .map(Statement::getResource)
            .collect(Collectors.toList());

        if (isTitlePath(path)) {
            if (entityResource.hasProperty(DCTerms.title)) {
                if (messages.contains("Property must not contain organization name")) {
                    violations.add(IndicatorType.titleNoOrgName);
                } else {
                    violations.add(IndicatorType.title);
                }
            } else {
                violations.add(IndicatorType.title);
                violations.add(IndicatorType.titleNoOrgName);
            }
        }

        if (isDescriptionPath(path)) {
            if (entityResource.hasProperty(DCTerms.description)) {
                if (messages.contains("Property must not share any values with dct:title") || messages.contains("Property must not contain the value of dct:title")) {
                    violations.add(IndicatorType.descriptionWithoutTitle);
                } else {
                    violations.add(IndicatorType.description);
                }
            } else {
                violations.add(IndicatorType.description);
                violations.add(IndicatorType.descriptionWithoutTitle);
            }
        }

        if (isDistributionPath(path) && !entityResource.hasProperty(DCAT.distribution)) {
            violations.add(IndicatorType.distributableData);
            violations.add(IndicatorType.controlledVocabularyUsage);
            violations.add(IndicatorType.licenseInformation);
        }

        if (hasControlledVocabularyViolations(distributions, otherReportEntries)) {
            violations.add(IndicatorType.controlledVocabularyUsage);
        }

        if (hasLicenseInformationViolations(distributions, otherReportEntries)) {
            violations.add(IndicatorType.licenseInformation);
        }

        if (hasDistributableDataViolations(distributions, otherReportEntries)) {
            violations.add(IndicatorType.distributableData);
        }

        if (isKeywordPath(path)) {
            violations.add(IndicatorType.keywordUsage);
        }

        if (isSubjectPath(path)) {
            violations.add(IndicatorType.subjectUsage);
        }

        if (isSpatialPath(path)) {
            violations.add(IndicatorType.geoSearch);
        }

        if (isContactPointPath(path) || isContactPointPropertyPath(path)) {
            violations.add(IndicatorType.contactPoint);
        }

        return violations;
    }

    private static Flux<Triple<Entity, Resource, Collection<ReportEntry>>> extractDatasetEntitiesFromModel(Model model, ValidationReport report) {
        Collection<ReportEntry> entries = report.getEntries();

        return Flux.defer(() ->
            Flux.fromIterable(model.listResourcesWithProperty(RDF.type, DCAT.Dataset).toList())
                .map(resource -> {
                        Entity entity = Entity.builder()
                            .id(extractFdkIdFromResource(resource))
                            .uri(resource.getURI())
                            .type(EntityType.DATASET)
                            .title(extractTitleFromResource(resource, DCTerms.title))
                            .catalog(extractCatalogFromModel(model, resource))
                            .contexts(extractContextsFromResource(resource))
                            .build();

                        Collection<ReportEntry> relatedReportEntries = entries
                            .parallelStream()
                            .filter(entry -> isReportEntryRelatedToEntityResource(entry, entity, resource))
                            .collect(Collectors.toList());

                        return Triple.of(entity, resource, relatedReportEntries);
                    }
                ))
            .subscribeOn(Schedulers.boundedElastic());
    }

}
