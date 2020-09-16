package no.fdk.repository;

import no.fdk.model.Assessment;
import no.fdk.model.EntityType;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

import java.util.Set;

public interface AssessmentRepository extends ReactiveMongoRepository<Assessment, String> {

    Flux<Assessment> findAllByEntityCatalogIdAndEntityType(String catalogId, EntityType entityType);

    Flux<Assessment> findAllByEntityCatalogUriAndEntityType(String catalogUri, EntityType entityType);

    Flux<Assessment> findAllByEntityUriIn(Set<String> entityUris);

}
