package no.fdk.service;

import lombok.extern.slf4j.Slf4j;
import no.fdk.model.MediaType;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static java.lang.String.format;

@Service
@Slf4j
public class IANAMediaTypeService {

    private static final String BASE_URI = "https://www.iana.org/assignments/media-types";

    private final Flux<String> registries = Flux.just(
        "application",
        "audio",
        "font",
        "image",
        "message",
        "model",
        "multipart",
        "text",
        "video"
    );

    public Flux<MediaType> harvestMediaTypes() {
        log.info("Starting harvest of IANA media types");

        AtomicInteger count = new AtomicInteger();

        return registries
            .flatMap(this::harvestMediaTypeRegistry)
            .doOnNext(mediaType -> count.getAndIncrement())
            .doFinally(signal -> log.info("Successfully harvested {} IANA media types", count.get()));
    }

    private Flux<MediaType> harvestMediaTypeRegistry(String registry) {
        return Mono.justOrEmpty(createRegistryUrl(registry))
            .flatMapMany(this::extractMediaTypeRegistryRecords)
            .map(name -> buildMediaType(registry, name));
    }

    private URL createRegistryUrl(String registry) {
        String url = format("%s/%s.csv", BASE_URI, registry);

        try {
            return new URL(url);
        } catch (MalformedURLException e) {
            log.error("Invalid IANA media type registry URL: {}", url, e);
        }

        return null;
    }

    private Flux<String> extractMediaTypeRegistryRecords(URL url) {
        try {
            CSVFormat format = CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withSkipHeaderRecord();
            Charset charset = Charset.defaultCharset();

            List<CSVRecord> records = CSVParser.parse(url, charset, format).getRecords();

            return Flux.fromIterable(records)
                .map(record -> record.get(0))
                .map(String::trim)
                .filter(Predicate.not(String::isEmpty));
        } catch (IOException e) {
            log.error("Failed to extract media type registry records from: {}", url.toString(), e);
        }

        return Flux.empty();
    }

    private MediaType buildMediaType(String registry, String name) {
        String code = format("%s/%s", registry, name);
        String uri = format("%s/%s", BASE_URI, code);

        return MediaType
            .builder()
            .uri(uri)
            .code(code)
            .build();
    }

}
