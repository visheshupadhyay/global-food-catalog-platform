package com.vishesh.foodcatalog.catalog.event.delivery;

import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import com.vishesh.foodcatalog.catalog.event.application.OutboxEventPublisher;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;

/** Sends an outbox payload to Kafka and reports delivery failures to the retry service. */
@Component
public class KafkaOutboxEventPublisher implements OutboxEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final String topic;
    private final long sendTimeoutMillis;

    public KafkaOutboxEventPublisher(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${catalog.kafka.menu-item-topic}") String topic,
            @Value("${catalog.kafka.send-timeout-ms:5000}") long sendTimeoutMillis) {
        if (kafkaTemplate == null) {
            throw new IllegalArgumentException("Kafka template cannot be null");
        }
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("Kafka topic cannot be blank");
        }
        if (sendTimeoutMillis <= 0) {
            throw new IllegalArgumentException("Kafka send timeout must be positive");
        }
        this.kafkaTemplate = kafkaTemplate;
        this.topic = topic;
        this.sendTimeoutMillis = sendTimeoutMillis;
    }


    @Override
    public void publish(OutboxEvent event) {
        if (event == null) {
            throw new IllegalArgumentException("Outbox event cannot be null");
        }

        try {
            kafkaTemplate.send(topic, event.getKafkaKey(), event.getPayload())
                    .get(sendTimeoutMillis, TimeUnit.MILLISECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Kafka publishing was interrupted", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("Kafka publishing failed", exception);
        }
    }
}
