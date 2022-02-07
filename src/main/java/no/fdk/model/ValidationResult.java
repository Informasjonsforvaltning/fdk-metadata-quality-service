package no.fdk.model;

import lombok.Builder;
import lombok.Data;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.shacl.ValidationReport;
import org.apache.jena.vocabulary.DCAT;
import org.apache.jena.vocabulary.RDF;

import static no.fdk.utils.AssessmentUtils.extractFdkIdFromResource;

@Data
@Builder
public class ValidationResult {
    private final String id;
    private final Model model;
    private final ValidationReport validationReport;

    public static ValidationResult create(Model entityModel, Model validationReportModel) {
        final String id = extractFdkIdFromResource(
                entityModel.listSubjectsWithProperty(RDF.type, DCAT.Dataset).next());
        return new ValidationResult(id, entityModel, ValidationReport.fromModel(validationReportModel));
    }
}
