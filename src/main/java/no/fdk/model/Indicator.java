package no.fdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Indicator {
    private IndicatorType type;
    private Integer weight;
    private Boolean conforms;
}
