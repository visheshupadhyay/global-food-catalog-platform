package com.vishesh.foodcatalog.catalog.application;

public class RestaurantChainNotFoundException extends RuntimeException{
    public RestaurantChainNotFoundException(String message) {
        super(message);
    }
}
