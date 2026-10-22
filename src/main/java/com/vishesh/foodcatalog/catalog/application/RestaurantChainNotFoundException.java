package com.vishesh.foodcatalog.catalog.application;

/** Tells the API that the requested parent restaurant chain does not exist. */
public class RestaurantChainNotFoundException extends RuntimeException{
    public RestaurantChainNotFoundException(String message) {
        super(message);
    }
}
