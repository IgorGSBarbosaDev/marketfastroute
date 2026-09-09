package com.marketfastroute.product;

import com.marketfastroute.shared.web.ApiExceptionHandler;
import com.marketfastroute.store.StoreNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ProductControllerTest {

    private final ProductService productService = mock(ProductService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new ProductController(productService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void listsProductsAsDtos() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(productService.findAvailableByStore(storeId, "milk"))
                .thenReturn(List.of(productResponse(productId)));

        mockMvc.perform(get("/api/v1/stores/{storeId}/products", storeId).param("search", "milk"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(productId.toString()))
                .andExpect(jsonPath("$[0].name").value("Milk"))
                .andExpect(jsonPath("$[0].sku").value("SKU-MILK"))
                .andExpect(jsonPath("$[0].ean").value("789000000001"))
                .andExpect(jsonPath("$[0].brand").value("Brand"))
                .andExpect(jsonPath("$[0].category").value("Dairy"))
                .andExpect(jsonPath("$[0].description").doesNotExist())
                .andExpect(jsonPath("$[0].active").doesNotExist());
    }

    @Test
    void returnsAProductById() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(productService.findById(storeId, productId)).thenReturn(productResponse(productId));

        mockMvc.perform(get("/api/v1/stores/{storeId}/products/{productId}", storeId, productId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(productId.toString()))
                .andExpect(jsonPath("$.name").value("Milk"));
    }

    @Test
    void returnsNotFoundWhenTheProductIsUnavailableInTheStore() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(productService.findById(storeId, productId))
                .thenThrow(new ProductNotFoundException(storeId, productId));

        mockMvc.perform(get("/api/v1/stores/{storeId}/products/{productId}", storeId, productId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("PRODUCT_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Product not found in store"))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    @Test
    void returnsNotFoundWhenTheStoreDoesNotExist() throws Exception {
        UUID storeId = UUID.randomUUID();
        when(productService.findAvailableByStore(storeId, null))
                .thenThrow(new StoreNotFoundException(storeId));

        mockMvc.perform(get("/api/v1/stores/{storeId}/products", storeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STORE_NOT_FOUND"));
    }

    @Test
    void returnsBadRequestWhenAPathUuidIsInvalid() throws Exception {
        mockMvc.perform(get("/api/v1/stores/not-a-uuid/products"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.message").value("Invalid request parameter"))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    private ProductResponse productResponse(UUID productId) {
        return new ProductResponse(productId, "Milk", "SKU-MILK", "789000000001", "Brand", "Dairy");
    }
}
