package no.fdk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fdk.model.MediaType;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.HashSet;
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

    private final Collection<MediaType> mediaTypes = new HashSet<>();

    public Flux<MediaType> getMediaTypes() {
        return Flux.fromIterable(mediaTypes)
            .switchIfEmpty(Mono.error(new IllegalStateException("Media types are not ready yet")));
    }

    @EventListener
    private void onReady(ApplicationReadyEvent event) {
        harvestMediaTypes();
    }

    @Scheduled(cron = "0 0 0 * * *")
    private void harvestMediaTypes() {
        log.info("Starting harvest of media types");

        AtomicInteger count = new AtomicInteger();

        Flux.merge(
            ianaMediaTypeService.harvestMediaTypes(),
            euMediaTypeService.harvestMediaTypes(),
            miscellaneousMediaTypes.map(code -> MediaType.builder().code(code).build())
        )
            .doFirst(mediaTypes::clear)
            .doOnNext(mediaType -> count.getAndIncrement())
            .doOnComplete(() -> log.info("Successfully harvested {} media types", count.get()))
            .subscribe(mediaTypes::add);
    }

}
