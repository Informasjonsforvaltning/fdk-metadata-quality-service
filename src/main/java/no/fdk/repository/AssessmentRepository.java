package no.fdk.repository;

import no.fdk.model.Assessment;
import no.fdk.model.Context;
import no.fdk.model.EntityType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.util.Collection;

public interface AssessmentRepository extends ReactiveMongoRepository<Assessment, String> {

    Flux<Assessment> findAllByEntityCatalogIdAndEntityTypeAndEntityContextsIn(String catalogId, EntityType entityType, Collection<Context> contexts);

    Flux<Assessment> findAllByEntityCatalogUriAndEntityTypeAndEntityContextsIn(String catalogUri, EntityType entityType, Collection<Context> contexts);

}
