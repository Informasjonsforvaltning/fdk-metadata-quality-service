package no.fdk.model;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Catalog {
    private String id;
    private String uri;
}
