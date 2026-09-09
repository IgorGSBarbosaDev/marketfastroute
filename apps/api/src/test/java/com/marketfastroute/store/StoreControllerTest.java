package com.marketfastroute.store;

import com.marketfastroute.shared.web.ApiExceptionHandler;
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

class StoreControllerTest {

    private final StoreService storeService = mock(StoreService.class);
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new StoreController(storeService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void listsStoresAsDtos() throws Exception {
        UUID storeId = UUID.randomUUID();
        when(storeService.findAll()).thenReturn(List.of(storeResponse(storeId)));

        mockMvc.perform(get("/api/v1/stores"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].id").value(storeId.toString()))
                .andExpect(jsonPath("$[0].name").value("Store One"))
                .andExpect(jsonPath("$[0].code").value("STORE-1"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].maps").doesNotExist())
                .andExpect(jsonPath("$[0].products").doesNotExist());
    }

    @Test
    void returnsStoreById() throws Exception {
        UUID storeId = UUID.randomUUID();
        when(storeService.findById(storeId)).thenReturn(storeResponse(storeId));

        mockMvc.perform(get("/api/v1/stores/{id}", storeId))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.id").value(storeId.toString()))
                .andExpect(jsonPath("$.address").value("Address"))
                .andExpect(jsonPath("$.city").value("City"))
                .andExpect(jsonPath("$.state").value("SP"));
    }

    @Test
    void returnsNotFoundWhenStoreDoesNotExist() throws Exception {
        UUID storeId = UUID.randomUUID();
        when(storeService.findById(storeId)).thenThrow(new StoreNotFoundException(storeId));

        mockMvc.perform(get("/api/v1/stores/{id}", storeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STORE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("Store not found"))
                .andExpect(jsonPath("$.details").isEmpty());
    }

    private StoreResponse storeResponse(UUID storeId) {
        return new StoreResponse(storeId, "Store One", "STORE-1", "Address", "City", "SP", true);
    }
}
