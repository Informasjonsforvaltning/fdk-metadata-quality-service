package no.fdk.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fdk.configuration.ApplicationProperties;
import no.fdk.model.EntityType;
import org.apache.jena.graph.Graph;
import org.apache.jena.rdfconnection.RDFConnection;
import org.apache.jena.rdfconnection.RDFConnectionFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import static java.lang.String.format;

@Slf4j
@Service
@RequiredArgsConstructor
public class SparqlService {

    private final ApplicationProperties applicationProperties;

    public Mono<Graph> getGraph(EntityType entityType) {
        String url = applicationProperties.getSparqlUri();

        try (RDFConnection connection = RDFConnectionFactory.connectFuseki(url)) {
            return Mono.justOrEmpty(
                connection
                    .queryDescribe(format("DESCRIBE ?s ?p ?o FROM <%s> WHERE { ?s ?p ?o }", entityType.getGraphName()))
                    .getGraph()
            );
        } catch (Exception e) {
            log.error("Failed to fetch RDF graph", e);

            return Mono.error(e);
        }
    }

}
