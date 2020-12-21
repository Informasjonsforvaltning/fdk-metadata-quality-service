package no.fdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Rating {
    private final Integer score;
    private final Integer maxScore;
    private final Integer satisfiedCriteria;
    private final Integer totalCriteria;
    private final RatingCategory category;
    private final Map<DimensionType, Rating> dimensionsRating;
}
