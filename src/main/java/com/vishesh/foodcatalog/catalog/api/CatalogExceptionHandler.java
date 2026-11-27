package com.vishesh.foodcatalog.catalog.api;

import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.vishesh.foodcatalog.catalog.application.DuplicateBusinessCodeException;
import com.vishesh.foodcatalog.catalog.application.DuplicateMenuGroupException;
import com.vishesh.foodcatalog.catalog.application.DuplicateMenuItemException;
import com.vishesh.foodcatalog.catalog.application.DuplicateOutletCodeException;
import com.vishesh.foodcatalog.catalog.application.MenuGroupNotFoundException;
import com.vishesh.foodcatalog.catalog.application.MenuGroupVersionConflictException;
import com.vishesh.foodcatalog.catalog.application.MenuItemNotFoundException;
import com.vishesh.foodcatalog.catalog.application.MenuItemVersionConflictException;
import com.vishesh.foodcatalog.catalog.application.OutletNotFoundException;
import com.vishesh.foodcatalog.catalog.application.OutletVersionConflictException;
import com.vishesh.foodcatalog.catalog.application.RestaurantChainNotFoundException;

import jakarta.servlet.http.HttpServletRequest;

/** Turns business failures into small, predictable HTTP error responses. */
@RestControllerAdvice
public class CatalogExceptionHandler {

	@ExceptionHandler(DuplicateBusinessCodeException.class)
	public ResponseEntity<ApiError> handleDuplicateBusinessCode(DuplicateBusinessCodeException exception,
			HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, exception, request);
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ApiError> handleValidationError(MethodArgumentNotValidException exception,
			HttpServletRequest request) {
		return error(exception.getStatusCode(), exception, request);
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<ApiError> handleIllegalArgument(IllegalArgumentException exception,
			HttpServletRequest request) {
		return error(HttpStatus.BAD_REQUEST, exception, request);
	}

	@ExceptionHandler(RestaurantChainNotFoundException.class)
	public ResponseEntity<ApiError> handleRestaurantChainNotFound(RestaurantChainNotFoundException exception,
			HttpServletRequest request) {
		return error(HttpStatus.NOT_FOUND, exception, request);
	}

	@ExceptionHandler(DuplicateMenuGroupException.class)
	public ResponseEntity<ApiError> handleDuplicateMenuGroup(DuplicateMenuGroupException exception,
			HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, exception, request);
	}

	@ExceptionHandler(DuplicateMenuItemException.class)
	public ResponseEntity<ApiError> handleDuplicateMenuItem(DuplicateMenuItemException exception,
			HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, exception, request);
	}

	@ExceptionHandler(DuplicateOutletCodeException.class)
	public ResponseEntity<ApiError> handleDuplicateOutletCode(DuplicateOutletCodeException exception,
			HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, exception, request);
	}

	@ExceptionHandler(OutletNotFoundException.class)
	public ResponseEntity<ApiError> handleOutletNotFound(OutletNotFoundException exception,
			HttpServletRequest request) {
		return error(HttpStatus.NOT_FOUND, exception, request);
	}

	@ExceptionHandler(OutletVersionConflictException.class)
	public ResponseEntity<ApiError> handleOutletVersionConflict(OutletVersionConflictException exception,
			HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, exception, request);
	}

	@ExceptionHandler(MenuGroupNotFoundException.class)
	public ResponseEntity<ApiError> handleMenuGroupNotFound(MenuGroupNotFoundException exception,
			HttpServletRequest request) {
		return error(HttpStatus.NOT_FOUND, exception, request);
	}

	@ExceptionHandler(MenuGroupVersionConflictException.class)
	public ResponseEntity<ApiError> handleMenuGroupVersionConflict(MenuGroupVersionConflictException exception,
			HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, exception, request);
	}

	@ExceptionHandler(MenuItemNotFoundException.class)
	public ResponseEntity<ApiError> handleMenuItemNotFound(MenuItemNotFoundException exception,
			HttpServletRequest request) {
		return error(HttpStatus.NOT_FOUND, exception, request);
	}

	@ExceptionHandler(MenuItemVersionConflictException.class)
	public ResponseEntity<ApiError> handleMenuItemVersionConflict(MenuItemVersionConflictException exception,
			HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, exception, request);
	}

	@ExceptionHandler(IllegalStateException.class)
	public ResponseEntity<ApiError> handleIllegalState(IllegalStateException exception, HttpServletRequest request) {
		return error(HttpStatus.CONFLICT, exception, request);
	}

	/** Builds the unchanged error body shared by every Catalog endpoint. */
	private ResponseEntity<ApiError> error(HttpStatusCode status, Exception exception, HttpServletRequest request) {
		ApiError body = new ApiError(status.value(), exception.getMessage(), request.getRequestURI());
		return ResponseEntity.status(status).body(body);
	}
}
