package com.marketfastroute.shared.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiExceptionHandlerTest {

	@Test
	void hidesInternalExceptionDetailsFromClient() {
		var handler = new ApiExceptionHandler();

		var response = handler.handleUnexpectedException(new IllegalStateException("database password"));

		assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
		var body = response.getBody();
		assertNotNull(body);
		assertEquals("INTERNAL_SERVER_ERROR", body.code());
		assertEquals("An unexpected error occurred", body.message());
		assertFalse(body.message().contains("database password"));
		assertEquals(0, body.details().size());
	}
}
