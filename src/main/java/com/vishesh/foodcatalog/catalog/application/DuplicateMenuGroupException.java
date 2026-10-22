package com.vishesh.foodcatalog.catalog.application;

/** Tells the API that this chain already has a menu group with that name. */
public class DuplicateMenuGroupException extends RuntimeException{

    public DuplicateMenuGroupException(String message) {
        super(message);
    }
}
