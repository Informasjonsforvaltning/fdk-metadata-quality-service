package no.fdk.model;

import lombok.RequiredArgsConstructor;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.vocabulary.DCAT;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
public enum EntityType {
    DATASET("dataset", DCAT.Dataset);

    private static final Map<String, EntityType> BY_LABEL = new HashMap<>();
    private static final Map<Resource, EntityType> BY_RESOURCE = new HashMap<>();

    static {
        Arrays.stream(EntityType.values())
            .forEach(value -> {
                BY_LABEL.put(value.label, value);
                BY_RESOURCE.put(value.resource, value);
            });
    }

    private final String label;
    private final Resource resource;

    public static EntityType valueOfLabel(String label) {
        return BY_LABEL.get(label);
    }

    public static EntityType valueOfAtomicNumber(Resource resource) {
        return BY_RESOURCE.get(resource);
    }

    public static Set<Resource> resources() {
        return BY_RESOURCE.keySet();
    }

    @Override
    public String toString() {
        return this.label;
    }
}
