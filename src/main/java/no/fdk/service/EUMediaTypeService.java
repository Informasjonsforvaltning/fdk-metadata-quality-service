package no.fdk.service;

import lombok.extern.slf4j.Slf4j;
import no.fdk.model.MediaType;
import org.apache.jena.rdf.model.Model;
import org.apache.jena.rdf.model.Resource;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.vocabulary.SKOS;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class EUMediaTypeService {

    private static final String BASE_URI = "http://publications.europa.eu/resource/authority/file-type";

    public Flux<MediaType> harvestMediaTypes() {
        log.info("Starting harvest of EU media types");

        Model model = RDFDataMgr.loadModel(BASE_URI, Lang.RDFXML);

        AtomicInteger count = new AtomicInteger();

        return Flux.fromIterable(model.listSubjectsWithProperty(SKOS.inScheme).toList())
            .filter(Resource::isURIResource)
            .map(Resource::getURI)
            .map(uri -> MediaType.builder().uri(uri).build())
            .doOnNext(mediaType -> count.getAndIncrement())
            .doFinally(signal -> log.info("Successfully harvested {} EU media types", count.get()));
    }

}
