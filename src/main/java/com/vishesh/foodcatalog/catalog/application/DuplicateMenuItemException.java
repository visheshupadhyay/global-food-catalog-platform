package com.vishesh.foodcatalog.catalog.application;

public class DuplicateMenuItemException extends RuntimeException {

    public DuplicateMenuItemException(String message) {
        super(message);
    }
    
}
