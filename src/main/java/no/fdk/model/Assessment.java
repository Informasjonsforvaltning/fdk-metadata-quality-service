package no.fdk.model;

import lombok.Builder;
import lombok.Data;

import java.util.Collection;

@Data
@Builder
public class Assessment {
    private String enityId;
    private EntityType entityType;
    private Rating rating;
    private Collection<Dimension> dimensions;
}
