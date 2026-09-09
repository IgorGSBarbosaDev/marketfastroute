package com.marketfastroute.shared.web;

import com.marketfastroute.store.StoreNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
