package com.vishesh.foodcatalog.catalog.monitoring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.kafka.KafkaContainer;
import org.testcontainers.utility.DockerImageName;
import org.springframework.boot.health.contributor.Status;

@Testcontainers
class KafkaHealthIndicatorTest {

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(
            DockerImageName.parse("apache/kafka:3.9.1"));

    @Test
    void health_whenKafkaIsReachable_returnsUp() {
        KafkaHealthIndicator indicator = new KafkaHealthIndicator(KAFKA.getBootstrapServers());

        assertEquals(Status.UP, indicator.health().getStatus());
    }

    @Test
    void health_whenKafkaIsUnavailable_returnsDown() {
        KafkaHealthIndicator indicator = new KafkaHealthIndicator("127.0.0.1:1");

        assertEquals(Status.DOWN, indicator.health().getStatus());
    }
}
