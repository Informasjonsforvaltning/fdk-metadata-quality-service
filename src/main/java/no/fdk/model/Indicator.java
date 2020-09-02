package no.fdk.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Indicator {
    private IndicatorType type;
    private Integer weight;
    private Boolean conforms;
}
