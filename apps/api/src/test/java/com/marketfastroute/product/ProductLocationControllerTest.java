package com.marketfastroute.product;

import com.marketfastroute.shared.web.ApiExceptionHandler;
import com.marketfastroute.store.StoreNotFoundException;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductLocationControllerTest {

    private final ProductLocationService productLocationService = mock(ProductLocationService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductLocationController(productLocationService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void listsProductLocationsAsDtos() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(productLocationService.findByProduct(storeId, productId))
                .thenReturn(List.of(locationResponse(locationId, productId, storeId, true)));

        mockMvc.perform(get("/api/v1/stores/{storeId}/products/{productId}/locations", storeId, productId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(locationId.toString()))
                .andExpect(jsonPath("$[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$[0].storeId").value(storeId.toString()))
                .andExpect(jsonPath("$[0].mapId").isNotEmpty())
                .andExpect(jsonPath("$[0].sector").value("Frozen"))
                .andExpect(jsonPath("$[0].aisle").value("Aisle 1"))
                .andExpect(jsonPath("$[0].shelfBlock").value("Shelf 1"))
                .andExpect(jsonPath("$[0].side").value("CENTER"))
                .andExpect(jsonPath("$[0].module").value("M-01"))
                .andExpect(jsonPath("$[0].shelfLevel").value(2))
                .andExpect(jsonPath("$[0].x").value(10.0))
                .andExpect(jsonPath("$[0].y").value(20.0))
                .andExpect(jsonPath("$[0].navigationNodeId").isNotEmpty())
                .andExpect(jsonPath("$[0].primaryLocation").value(true))
                .andExpect(jsonPath("$[0].active").doesNotExist());
    }

    @Test
    void returnsThePrimaryLocation() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(productLocationService.findPrimaryByProduct(storeId, productId))
                .thenReturn(locationResponse(locationId, productId, storeId, true));

        mockMvc.perform(get("/api/v1/stores/{storeId}/products/{productId}/locations/primary", storeId, productId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(locationId.toString()))
                .andExpect(jsonPath("$.primaryLocation").value(true));
    }

    @Test
    void returnsAnIndividualLocation() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(productLocationService.findById(storeId, productId, locationId))
                .thenReturn(locationResponse(locationId, productId, storeId, false));

        mockMvc.perform(get(
                        "/api/v1/stores/{storeId}/products/{productId}/locations/{locationId}",
                        storeId, productId, locationId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(locationId.toString()))
                .andExpect(jsonPath("$.primaryLocation").value(false));
    }

    @Test
    void returnsNotFoundWhenTheStoreDoesNotExist() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(productLocationService.findByProduct(storeId, productId))
                .thenThrow(new StoreNotFoundException(storeId));

        mockMvc.perform(get("/api/v1/stores/{storeId}/products/{productId}/locations", storeId, productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STORE_NOT_FOUND"))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void returnsNotFoundWhenTheLocationDoesNotExist() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID locationId = UUID.randomUUID();
        when(productLocationService.findById(storeId, productId, locationId))
                .thenThrow(new ProductLocationNotFoundException(storeId, productId, locationId));

        mockMvc.perform(get(
                        "/api/v1/stores/{storeId}/products/{productId}/locations/{locationId}",
                        storeId, productId, locationId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_LOCATION_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Product location not found"));
    }

    @Test
    void returnsNotFoundWhenThePrimaryLocationDoesNotExist() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(productLocationService.findPrimaryByProduct(storeId, productId))
                .thenThrow(new ProductLocationNotFoundException(storeId, productId));

        mockMvc.perform(get("/api/v1/stores/{storeId}/products/{productId}/locations/primary", storeId, productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_LOCATION_NOT_FOUND"));
    }

    @Test
    void hidesTheReasonWhenLocationDataIsInconsistent() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(productLocationService.findByProduct(storeId, productId))
                .thenThrow(new ProductLocationConsistencyException(
                        UUID.randomUUID(), "map belongs to another store"));

        mockMvc.perform(get("/api/v1/stores/{storeId}/products/{productId}/locations", storeId, productId))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("PRODUCT_LOCATION_INCONSISTENT"))
                .andExpect(jsonPath("$.message").value("Product location data is inconsistent"))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void returnsNotFoundWhenTheProductIsNotAssociatedWithTheStore() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(productLocationService.findByProduct(storeId, productId))
                .thenThrow(new ProductNotFoundException(storeId, productId));

        mockMvc.perform(get("/api/v1/stores/{storeId}/products/{productId}/locations", storeId, productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"));
    }

    @Test
    void returnsBadRequestWhenAPathUuidIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/stores/not-a-uuid/products/{productId}/locations", UUID.randomUUID()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message").value("Invalid request parameter"))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    private ProductLocationResponse locationResponse(
            UUID locationId,
            UUID productId,
            UUID storeId,
            boolean primary
    ) {
        return new ProductLocationResponse(
                locationId,
                productId,
                storeId,
                UUID.randomUUID(),
                "Frozen",
                "Aisle 1",
                "Shelf 1",
                ProductLocationSide.CENTER,
                "M-01",
                2,
                new BigDecimal("10.0000"),
                new BigDecimal("20.0000"),
                UUID.randomUUID(),
                primary
        );
    }
}
