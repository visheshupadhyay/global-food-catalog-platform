package com.vishesh.foodcatalog.catalog.event.monitoring;

public interface OutboxMetricsReader {
    OutboxMetrics readMetrics() ;
}
