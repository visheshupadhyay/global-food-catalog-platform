package com.vishesh.foodcatalog.catalog.application;

/**
 * Says that another restaurant chain already uses this business code.
 * A business code is like a unique nickname for a chain.
 */
public class DuplicateBusinessCodeException extends RuntimeException {

    public DuplicateBusinessCodeException(String string) {
        super(string);
    }
    
}
