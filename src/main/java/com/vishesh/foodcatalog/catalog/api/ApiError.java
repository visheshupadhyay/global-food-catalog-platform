package com.vishesh.foodcatalog.catalog.api;

/**
 * A consistent note returned when the API rejects a request.
 */
public record ApiError(int status, String message, String path) {
}
