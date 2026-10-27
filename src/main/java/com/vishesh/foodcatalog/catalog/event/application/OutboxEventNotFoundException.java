package com.vishesh.foodcatalog.catalog.event.application;

/** Raised when an outbox row required by an operation does not exist. */
public class OutboxEventNotFoundException extends RuntimeException {
    public OutboxEventNotFoundException(Long id) {
        super("Outbox event was not found: " + id);
    }

    public OutboxEventNotFoundException(String string) {
        super(string);
    }
}
