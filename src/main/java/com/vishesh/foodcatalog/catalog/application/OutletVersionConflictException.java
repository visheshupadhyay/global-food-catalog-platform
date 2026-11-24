package com.vishesh.foodcatalog.catalog.application;

/** Raised when an admin activates an outlet using an old representation. */
public class OutletVersionConflictException extends RuntimeException {
    public OutletVersionConflictException(String message) {
        super(message);
    }
}
