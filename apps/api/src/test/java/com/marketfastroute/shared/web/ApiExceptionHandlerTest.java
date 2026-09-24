package com.marketfastroute.shared.web;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import com.marketfastroute.map.MapPublicationException;
import com.marketfastroute.map.MapPublicationIssue;

import java.util.List;

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

	@Test
	void returnsActionableMapIssuesWhenPublicationFails() {
		var issue = new MapPublicationIssue("ROUTE_ENDPOINT_MISSING", "Configure an entrance", "entrance", null);
		var response = new ApiExceptionHandler().handleMapPublication(new MapPublicationException(List.of(issue)));

		assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, response.getStatusCode());
		assertNotNull(response.getBody());
		assertEquals("MAP_NOT_PUBLISHABLE", response.getBody().code());
		assertEquals(List.of(issue), response.getBody().details().get("issues"));
	}
}
