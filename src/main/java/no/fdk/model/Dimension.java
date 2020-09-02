package no.fdk.model;

import lombok.Builder;
import lombok.Data;

import java.util.Collection;

@Data
@Builder
public class Dimension {
    private DimensionType type;
    private Rating rating;
    private Collection<Indicator> indicators;
}
