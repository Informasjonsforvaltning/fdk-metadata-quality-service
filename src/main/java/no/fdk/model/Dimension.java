package no.fdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.Collection;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Dimension {
    private final DimensionType type;
    private final Rating rating;
    private final Collection<Indicator> indicators;
}
