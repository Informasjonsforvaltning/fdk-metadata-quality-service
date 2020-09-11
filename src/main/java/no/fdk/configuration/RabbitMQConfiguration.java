package no.fdk.configuration;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfiguration {

    @Bean
    public Queue updatesQueue() {
        return new AnonymousQueue();
    }

    @Bean
    public TopicExchange updatesTopicExchange() {
        return new TopicExchange("updates", false, false);
    }

    @Bean
    Binding updatesBinding(Queue queue, TopicExchange topicExchange) {
        return BindingBuilder
            .bind(queue)
            .to(topicExchange)
            .with("assessments.update");
    }

}
