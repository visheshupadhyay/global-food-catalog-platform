package com.vishesh.foodcatalog.catalog.event.delivery;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import com.vishesh.foodcatalog.catalog.event.domain.OutboxEvent;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventStatus;
import com.vishesh.foodcatalog.catalog.event.domain.OutboxEventType;

@ExtendWith(MockitoExtension.class)
public class KafkaOutboxEventPublisherTest {
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private KafkaOutboxEventPublisher publisher;

    private static final Instant NOW = Instant.parse("2026-01-01T00:00:00Z");

    @BeforeEach
    void test() {
        publisher = new KafkaOutboxEventPublisher(kafkaTemplate,
                "catalog.menu-item.v1",
                5000);
    }

    @Test
    void publish_whenKafkaAcknowledges_sendsTopicKeyAndPayload() {
        CompletableFuture<SendResult<String, String>> successfulFuture = CompletableFuture.completedFuture(null);
        OutboxEvent event = event(101L);
        when(kafkaTemplate.send(
                eq("catalog.menu-item.v1"),
                eq(event.getKafkaKey()),
                eq(event.getPayload())))
                .thenReturn(successfulFuture);

        assertDoesNotThrow(() -> publisher.publish(event));

        verify(kafkaTemplate).send("catalog.menu-item.v1", event.getKafkaKey(), event.getPayload());
    }

    @Test
    void publish_whenKafkaFails_throwsException() {
        OutboxEvent event = event(101L);
        CompletableFuture<SendResult<String, String>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IllegalStateException("Kafka unavailable"));
        when(kafkaTemplate.send(eq("catalog.menu-item.v1"), eq(event.getKafkaKey()), eq(event.getPayload())))
                .thenReturn(failedFuture);

        assertThrows(IllegalStateException.class, () -> publisher.publish(event));
        verify(kafkaTemplate).send("catalog.menu-item.v1", event.getKafkaKey(), event.getPayload());
    }

    @Test
    void publish_whenKafkaDoesNotRespond_throwsException() {
        KafkaOutboxEventPublisher shortTimeoutPublisher = new KafkaOutboxEventPublisher(
                kafkaTemplate, "catalog.menu-item.v1", 1L);
        OutboxEvent event = event(101L);
        when(kafkaTemplate.send(eq("catalog.menu-item.v1"), eq(event.getKafkaKey()), eq(event.getPayload())))
                .thenReturn(new CompletableFuture<>());

        assertThrows(IllegalStateException.class, () -> shortTimeoutPublisher.publish(event));
    }

    @Test
    void publish_withNullEvent_rejectsInputWithoutCallingKafka() {
        assertThrows(IllegalArgumentException.class, () -> publisher.publish(null));
        verify(kafkaTemplate, never()).send(org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void publish_whenThreadIsInterrupted_restoresInterruptFlag() {
        OutboxEvent event = event(101L);
        when(kafkaTemplate.send(eq("catalog.menu-item.v1"), eq(event.getKafkaKey()), eq(event.getPayload())))
                .thenReturn(new CompletableFuture<>());

        Thread.currentThread().interrupt();
        try {
            assertThrows(IllegalStateException.class, () -> publisher.publish(event));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }

    @Test
    void constructor_withNullTemplate_rejectsInput() {
        assertThrows(IllegalArgumentException.class,
                () -> new KafkaOutboxEventPublisher(null, "catalog.menu-item.v1", 5000L));
    }

    @Test
    void constructor_withBlankTopic_rejectsInput() {
        assertThrows(IllegalArgumentException.class,
                () -> new KafkaOutboxEventPublisher(kafkaTemplate, " ", 5000L));
    }

    @Test
    void constructor_withNonPositiveTimeout_rejectsInput() {
        assertThrows(IllegalArgumentException.class,
                () -> new KafkaOutboxEventPublisher(kafkaTemplate, "catalog.menu-item.v1", 0L));
        assertThrows(IllegalArgumentException.class,
                () -> new KafkaOutboxEventPublisher(kafkaTemplate, "catalog.menu-item.v1", -1L));
    }

    private OutboxEvent event(Long id) {
        return new OutboxEvent(
                id,
                UUID.randomUUID(),
                null,
                OutboxEventType.MENU_ITEM_CHANGED,
                "menu-item-" + id,
                "{\"itemId\":" + id + "}",
                OutboxEventStatus.PENDING,
                0L,
                null,
                null,
                NOW,
                null,
                null,
                id,
                1L);
    }
}
