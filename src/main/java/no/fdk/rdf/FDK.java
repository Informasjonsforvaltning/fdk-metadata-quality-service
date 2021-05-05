package no.fdk.rdf;

import org.apache.jena.rdf.model.Property;
import org.apache.jena.rdf.model.ResourceFactory;

public class FDK {
    public static final String NAME = "FDK";
    public static final String NS = "https://raw.githubusercontent.com/Informasjonsforvaltning/fdk-data-transformation-service/master/src/main/resources/ontology/fdk.owl#";
    public static final String PREFIX = "fdk";

    public static final Property isRelatedToTransportportal = ResourceFactory.createProperty(uri("isRelatedToTransportportal"));

    private FDK() {}

    public static String uri(String name) {
        return NS + name;
    }
}
