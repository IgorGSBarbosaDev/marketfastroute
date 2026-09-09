package com.marketfastroute.routing;

import com.marketfastroute.shared.web.ApiExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RouteControllerTest {

    private final RouteService routeService = mock(RouteService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new RouteController(routeService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void returnsTheRouteResponseAsDtos() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        UUID entranceId = UUID.randomUUID();
        UUID productNodeId = UUID.randomUUID();
        UUID checkoutId = UUID.randomUUID();
        when(routeService.calculate(new RouteRequest(storeId, List.of(productId))))
                .thenReturn(new RouteResponse(
                        storeId,
                        mapId,
                        List.of(new RouteStopResponse(productId, "Milk", locationId, productNodeId, 1)),
                        List.of(
                                new RoutePathNodeResponse(entranceId, BigDecimal.ZERO, BigDecimal.ZERO),
                                new RoutePathNodeResponse(productNodeId, BigDecimal.ONE, BigDecimal.ZERO),
                                new RoutePathNodeResponse(checkoutId, BigDecimal.TEN, BigDecimal.ZERO)
                        ),
                        new BigDecimal("12.5000")
                ));

        mockMvc.perform(post("/api/v1/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":\"" + storeId + "\",\"productIds\":[\"" + productId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.mapId").value(mapId.toString()))
                .andExpect(jsonPath("$.orderedStops", hasSize(1)))
                .andExpect(jsonPath("$.orderedStops[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.orderedStops[0].productName").value("Milk"))
                .andExpect(jsonPath("$.orderedStops[0].productLocationId").value(locationId.toString()))
                .andExpect(jsonPath("$.orderedStops[0].navigationNodeId").value(productNodeId.toString()))
                .andExpect(jsonPath("$.orderedStops[0].order").value(1))
                .andExpect(jsonPath("$.path", hasSize(3)))
                .andExpect(jsonPath("$.path[0].nodeId").value(entranceId.toString()))
                .andExpect(jsonPath("$.path[1].x").value(1.0))
                .andExpect(jsonPath("$.distanceMeters").value(12.5))
                .andExpect(jsonPath("$.active").doesNotExist());
    }

    @Test
    void returnsBadRequestForAnInvalidRouteRequest() throws Exception {
        mockMvc.perform(post("/api/v1/routes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"storeId\":null,\"productIds\":[]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
    }
}
