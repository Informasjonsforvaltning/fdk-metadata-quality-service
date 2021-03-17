package no.fdk.utils;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.jena.riot.Lang;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class LanguageUtils {

    public static Lang mediaTypeToRdfLanguage(String mediaType) {
        if (StringUtils.containsIgnoreCase(mediaType, Lang.TURTLE.getHeaderString())) {
            return Lang.TURTLE;
        } else if (StringUtils.containsIgnoreCase(mediaType, Lang.RDFJSON.getHeaderString())) {
            return Lang.RDFJSON;
        } else if (StringUtils.containsIgnoreCase(mediaType, Lang.JSONLD.getHeaderString())) {
            return Lang.JSONLD;
        } else if (StringUtils.containsIgnoreCase(mediaType, Lang.RDFXML.getHeaderString())) {
            return Lang.RDFXML;
        }

        return Lang.TURTLE;
    }

}
