package no.fdk.utils;

import org.apache.commons.lang3.StringUtils;
import org.apache.jena.riot.Lang;

public class LanguageUtils {

    public static Lang mediaTypeToRdfLanguage(String mediaType) {
        if (StringUtils.containsIgnoreCase(mediaType, "text/turtle")) {
            return Lang.TURTLE;
        } else if (StringUtils.containsIgnoreCase(mediaType, "application/rdf+json")) {
            return Lang.RDFJSON;
        } else if (StringUtils.containsIgnoreCase(mediaType, "application/ld+json")) {
            return Lang.JSONLD;
        } else if (StringUtils.containsIgnoreCase(mediaType, "application/rdf+xml")) {
            return Lang.RDFXML;
        }

        return Lang.TURTLE;
    }

}
