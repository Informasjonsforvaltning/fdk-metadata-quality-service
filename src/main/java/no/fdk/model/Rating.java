package no.fdk.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Rating {
    private Integer score;
    private Integer maxScore;
    private Integer satisfiedCriteria;
    private Integer totalCriteria;
    private RatingCategory category;
}
