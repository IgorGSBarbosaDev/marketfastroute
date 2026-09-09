package com.marketfastroute.shared.web;

import com.marketfastroute.product.ProductNotFoundException;
import com.marketfastroute.product.ProductLocationConsistencyException;
import com.marketfastroute.product.ProductLocationNotFoundException;
import com.marketfastroute.store.StoreNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

	@ExceptionHandler(StoreNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleStoreNotFound(StoreNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiErrorResponse(
						"STORE_NOT_FOUND",
						"Store not found",
						Map.of()
				));
	}

	@ExceptionHandler(ProductNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleProductNotFound(ProductNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiErrorResponse(
						"PRODUCT_NOT_FOUND",
						"Product not found in store",
						Map.of()
				));
	}

	@ExceptionHandler(ProductLocationNotFoundException.class)
	public ResponseEntity<ApiErrorResponse> handleProductLocationNotFound(ProductLocationNotFoundException exception) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
				.body(new ApiErrorResponse(
						"PRODUCT_LOCATION_NOT_FOUND",
						"Product location not found",
						Map.of()
				));
	}

	@ExceptionHandler(ProductLocationConsistencyException.class)
	public ResponseEntity<ApiErrorResponse> handleProductLocationConsistency(
			ProductLocationConsistencyException exception
	) {
		log.error("Product location data is inconsistent", exception);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse(
						"PRODUCT_LOCATION_INCONSISTENT",
						"Product location data is inconsistent",
						Map.of()
				));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ApiErrorResponse> handleInvalidParameter(MethodArgumentTypeMismatchException exception) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST)
				.body(new ApiErrorResponse(
						"INVALID_PARAMETER",
						"Invalid request parameter",
						Map.of()
				));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<ApiErrorResponse> handleUnexpectedException(Exception exception) {
		log.error("Unhandled request error", exception);

		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
				.body(new ApiErrorResponse(
						"INTERNAL_SERVER_ERROR",
						"An unexpected error occurred",
						Map.of()
				));
	}
}
