package com.vishesh.foodcatalog.catalog.application;

/** Raised when an update is based on an older menu-item version. */
public class MenuItemVersionConflictException extends RuntimeException {
    public MenuItemVersionConflictException(String message) {
        super(message);
    }
}
