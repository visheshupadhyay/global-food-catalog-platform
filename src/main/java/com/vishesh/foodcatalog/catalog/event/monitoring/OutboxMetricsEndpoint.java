package com.vishesh.foodcatalog.catalog.event.monitoring;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/actuator/outbox")
/** Exposes durable outbox health for operators and monitoring tools. */
public class OutboxMetricsEndpoint {
    private final OutboxMetricsReader reader;

    public OutboxMetricsEndpoint(
            @Qualifier("outboxMetricsPersistenceAdapter") OutboxMetricsReader reader) {
        this.reader = reader;
    }

    @GetMapping
    public OutboxMetrics readMetrics() {
        return reader.readMetrics();
    }
}
