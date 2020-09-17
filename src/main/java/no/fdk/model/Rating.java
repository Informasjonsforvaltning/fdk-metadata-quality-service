package no.fdk.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Rating {
    private Integer score;
    private Integer maxScore;
    private Integer satisfiedCriteria;
    private Integer totalCriteria;
    private RatingCategory category;
}
