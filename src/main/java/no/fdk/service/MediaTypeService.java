package no.fdk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fdk.model.MediaType;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaTypeService {

    private final IANAMediaTypeService ianaMediaTypeService;
    private final EUMediaTypeService euMediaTypeService;

    private final Flux<String> miscellaneousMediaTypes = Flux.just(
        "application/x.siri",
        "application/x.gtfs",
        "application/gpx+xml",
        "application/x.gtfsrt",
        "application/x.ualf",
        "application/x-ogc-sosi",
        "application/x.yaml",
        "application/vnd.sealed-xls",
        "application/x.netex",
        "application/x.wfs",
        "application/x.wms",
        "text/plain"
    );

    public Flux<MediaType> getMediaTypes() {
        log.info("Starting harvest of media types");

        AtomicInteger count = new AtomicInteger();

        return Flux.merge(
            ianaMediaTypeService.harvestMediaTypes(),
            euMediaTypeService.harvestMediaTypes(),
            miscellaneousMediaTypes.map(code -> MediaType.builder().code(code).build())
        )
            .doOnNext(mediaType -> count.getAndIncrement())
            .doFinally(signal -> log.info("Successfully harvested {} media types", count.get()));
    }

}
