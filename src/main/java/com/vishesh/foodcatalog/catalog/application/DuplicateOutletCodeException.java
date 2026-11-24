package com.vishesh.foodcatalog.catalog.application;

/** Raised when a chain already owns an outlet with the requested outlet code. */
public class DuplicateOutletCodeException extends RuntimeException {
    public DuplicateOutletCodeException(String message) {
        super(message);
    }
}
