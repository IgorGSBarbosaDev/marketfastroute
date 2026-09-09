package com.marketfastroute.shared.web;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;

class CorrelationIdFilterTest {

	private final CorrelationIdFilter filter = new CorrelationIdFilter();

	@Test
	void generatesCorrelationIdWhenRequestDoesNotProvideOne() throws Exception {
		var request = new MockHttpServletRequest();
		var response = new MockHttpServletResponse();

		filter.doFilter(request, response, noOpChain());

		String correlationId = response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER);
		assertNotNull(correlationId);
		assertFalse(correlationId.isBlank());
	}

	@Test
	void propagatesCorrelationIdProvidedByRequest() throws Exception {
		var request = new MockHttpServletRequest();
		var response = new MockHttpServletResponse();
		request.addHeader(CorrelationIdFilter.CORRELATION_ID_HEADER, "request-id-123");

		filter.doFilter(request, response, noOpChain());

		assertEquals("request-id-123", response.getHeader(CorrelationIdFilter.CORRELATION_ID_HEADER));
	}

	private FilterChain noOpChain() {
		return (request, response) -> { };
	}
}
