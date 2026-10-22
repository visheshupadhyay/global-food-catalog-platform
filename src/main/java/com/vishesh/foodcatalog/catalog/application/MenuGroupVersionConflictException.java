package com.vishesh.foodcatalog.catalog.application;

/** Tells us that someone tried to update an older copy of a menu group. */
public class MenuGroupVersionConflictException extends RuntimeException {
    public MenuGroupVersionConflictException(String message) {
        super(message);
    }
}
