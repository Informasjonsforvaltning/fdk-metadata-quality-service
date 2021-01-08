package no.fdk.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fdk.configuration.ApplicationProperties;
import no.fdk.model.EntityType;
import no.fdk.service.AssessmentService;
import no.fdk.utils.GraphUtils;
import org.apache.jena.riot.Lang;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static java.lang.String.format;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQListener {

    private final ApplicationProperties applicationProperties;
    private final AssessmentService assessmentService;
    private final WebClient webClient = WebClient
        .builder()
        .codecs(clientCodecConfigurer -> clientCodecConfigurer.defaultCodecs().maxInMemorySize(20000000))
        .build();

    @RabbitListener(queues = "#{updatesQueue.name}")
    private void receiveMessage(Message message) {
        log.info("Received message with key: {}", message.getMessageProperties().getReceivedRoutingKey());

        String url = format("%s/catalogs", applicationProperties.getDatasetHarvesterBaseUri());

        webClient
            .get()
            .uri(url)
            .retrieve()
            .bodyToMono(String.class)
            .flatMap(body -> Mono.just(GraphUtils.stringToGraph(body, Lang.TURTLE)))
            .flatMapMany(graph -> assessmentService.assess(graph, EntityType.dataset))
            .doOnNext(assessmentService::upsertAssessment)
            .count()
            .doOnSuccess(count -> log.info("Finished creating {} assessments", count))
            .doOnError(Throwable::printStackTrace)
            .subscribe();
    }

}
