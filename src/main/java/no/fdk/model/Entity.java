package no.fdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Entity {
    private final String uri;
    private final EntityType type;
    private final Map<String, String> title;
    private final Catalog catalog;
}
