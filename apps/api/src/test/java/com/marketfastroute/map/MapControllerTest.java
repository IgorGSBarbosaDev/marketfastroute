package com.marketfastroute.map;

import com.marketfastroute.shared.web.ApiExceptionHandler;
import com.marketfastroute.store.StoreNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class MapControllerTest {

    @Mock
    private MapService mapService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new MapController(mapService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void returnsTheCompleteMapDto() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();
        UUID nodeId = UUID.randomUUID();
        when(mapService.findActiveByStore(storeId)).thenReturn(new StoreMapResponse(
                mapId,
                storeId,
                3,
                "Main map",
                new BigDecimal("100.0000"),
                new BigDecimal("80.0000"),
                new BigDecimal("1.000000"),
                List.of(new SectorResponse(
                        sectorId, "Dairy", "DAIRY", BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO)),
                List.of(),
                List.of(),
                List.of(),
                List.of(new MapNodeResponse(
                        nodeId, MapNodeType.ENTRANCE, BigDecimal.ZERO, BigDecimal.ZERO, "Entrance")),
                List.of()
        ));

        mockMvc.perform(get("/api/v1/stores/{storeId}/map", storeId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mapId.toString()))
                .andExpect(jsonPath("$.storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.version").value(3))
                .andExpect(jsonPath("$.sectors[0].id").value(sectorId.toString()))
                .andExpect(jsonPath("$.nodes[0].id").value(nodeId.toString()))
                .andExpect(jsonPath("$.edges").isArray());
    }

    @Test
    void returnsNotFoundWhenTheStoreDoesNotExist() throws Exception {
        UUID storeId = UUID.randomUUID();
        when(mapService.findActiveByStore(storeId)).thenThrow(new StoreNotFoundException(storeId));

        mockMvc.perform(get("/api/v1/stores/{storeId}/map", storeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STORE_NOT_FOUND"));
    }

    @Test
    void returnsNotFoundWhenTheStoreHasNoActiveMap() throws Exception {
        UUID storeId = UUID.randomUUID();
        when(mapService.findActiveByStore(storeId))
                .thenThrow(new StoreMapNotFoundException(storeId));

        mockMvc.perform(get("/api/v1/stores/{storeId}/map", storeId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("STORE_MAP_NOT_FOUND"));
    }

    @Test
    void returnsBadRequestWhenTheStoreIdIsNotAUuid() throws Exception {
        mockMvc.perform(get("/api/v1/stores/not-a-uuid/map"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }
}
