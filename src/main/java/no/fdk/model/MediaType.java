package no.fdk.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MediaType {
    private final String uri;
    private final String code;
}
