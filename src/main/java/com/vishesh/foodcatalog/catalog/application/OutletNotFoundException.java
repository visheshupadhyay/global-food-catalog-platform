package com.vishesh.foodcatalog.catalog.application;

public class OutletNotFoundException extends RuntimeException {
    public OutletNotFoundException(String message) {
        super(message);
    }
}
