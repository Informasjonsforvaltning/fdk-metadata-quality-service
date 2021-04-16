package no.fdk.repository;

import no.fdk.model.Assessment;
import no.fdk.model.Context;
import no.fdk.model.EntityType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Set;

public interface AssessmentRepository extends ReactiveMongoRepository<Assessment, String> {

    Mono<Long> countByEntityCatalogIdAndEntityTypeAndEntityContextsIn(String catalogId, EntityType entityType, Collection<Context> contexts);

    Flux<Assessment> findAllByEntityCatalogIdAndEntityTypeAndEntityContextsIn(String catalogId, EntityType entityType, Collection<Context> contexts);

    Flux<Assessment> findAllByEntityCatalogIdAndEntityTypeAndEntityContextsIn(String catalogId, EntityType entityType, Collection<Context> contexts, Pageable pageable);

    Flux<Assessment> findAllByEntityCatalogUriAndEntityTypeAndEntityContextsIn(String catalogUri, EntityType entityType, Collection<Context> contexts);

    Flux<Assessment> findAllByEntityUriIn(Set<String> entityUris);

}
