package no.fdk.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import no.fdk.model.EntityType;
import no.fdk.service.AssessmentService;
import no.fdk.service.SparqlService;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class RabbitMQListener {

    private final AssessmentService assessmentService;
    private final SparqlService sparqlService;

    @RabbitListener(queues = "#{updatesQueue.name}")
    private void receiveMessage(Message message) {
        log.info("Received message with key: {}", message.getMessageProperties().getReceivedRoutingKey());

        sparqlService
            .getGraph(EntityType.DATASET)
            .flatMapMany(graph -> assessmentService.assess(graph, EntityType.DATASET))
            .doOnNext(assessmentService::upsertAssessment)
            .count()
            .onErrorReturn(0L)
            .doOnSuccess(count -> log.info("Finished creating {} assessments", count))
            .doOnError(throwable -> log.error("Failed to process message and create assessments", throwable))
            .subscribe();
    }

}
