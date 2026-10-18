package com.vishesh.foodcatalog.catalog.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vishesh.foodcatalog.catalog.application.DuplicateBusinessCodeException;
import com.vishesh.foodcatalog.catalog.application.RestaurantChainNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

@RestControllerAdvice
public class CatalogExceptionHandler {
    // Converts Java exceptions into simple HTTP error responses for API clients.

    @ExceptionHandler(DuplicateBusinessCodeException.class)
    public ResponseEntity<ApiError> handleDuplicateBusinessCode(
            DuplicateBusinessCodeException exception,
            HttpServletRequest servletRequest) {

        // A duplicate conflicts with data that already exists.
        int status = HttpStatus.CONFLICT.value();
        String message = exception.getMessage();
        String path = servletRequest.getRequestURI();

        ApiError apiError = new ApiError(status, message, path);
        return ResponseEntity.status(status).body(apiError);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> handleValidationError(
            MethodArgumentNotValidException exception,
            HttpServletRequest servletRequest) {

        // Invalid request fields should be reported as Bad Request.
        int status = exception.getStatusCode().value();
        String message = exception.getMessage();
        String path = servletRequest.getRequestURI();

        ApiError apiError = new ApiError(status, message, path);
        return ResponseEntity.status(exception.getStatusCode()).body(apiError);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgument(
            IllegalArgumentException exception,
            HttpServletRequest servletRequest) {

        // The service uses this exception when a business argument is invalid.
        int status = HttpStatus.BAD_REQUEST.value();
        String message = exception.getMessage();
        String path = servletRequest.getRequestURI();

        ApiError apiError = new ApiError(status, message, path);
        return ResponseEntity.status(status).body(apiError);
    }

    @ExceptionHandler(RestaurantChainNotFoundException.class)
    public ResponseEntity<ApiError> handleRestaurantChainNotFound(
            RestaurantChainNotFoundException exception,
            HttpServletRequest servletRequest) {

        // The requested restaurant chain does not exist, so the API returns 404.
        int status = HttpStatus.NOT_FOUND.value();
        String message = exception.getMessage();
        String path = servletRequest.getRequestURI();

        ApiError apiError = new ApiError(status, message, path);
        return ResponseEntity.status(status).body(apiError);
    }
}
