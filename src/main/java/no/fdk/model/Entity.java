package no.fdk.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Entity {
    private String uri;
    private EntityType type;
    private Catalog catalog;
}
