package com.marketfastroute.admin;

import com.marketfastroute.admin.dto.StoreAdminResponse;
import com.marketfastroute.admin.dto.StoreMapAdminResponse;
import com.marketfastroute.map.MapGraphAdminService;
import com.marketfastroute.map.MapNodeType;
import com.marketfastroute.map.MapStructureAdminService;
import com.marketfastroute.map.MapStatus;
import com.marketfastroute.shared.web.ApiExceptionHandler;
import com.marketfastroute.store.StoreAdminService;
import com.marketfastroute.store.StoreMapAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdminControllerTest {

    private final StoreAdminService storeAdminService = mock(StoreAdminService.class);
    private final StoreMapAdminService storeMapAdminService = mock(StoreMapAdminService.class);
    private final MapStructureAdminService mapStructureAdminService = mock(MapStructureAdminService.class);
    private final MapGraphAdminService mapGraphAdminService = mock(MapGraphAdminService.class);

    private MockMvc storeMockMvc;
    private MockMvc mapMockMvc;

    @BeforeEach
    void setUp() {
        storeMockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminStoreController(storeAdminService, storeMapAdminService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
        mapMockMvc = MockMvcBuilders.standaloneSetup(
                        new AdminMapController(mapStructureAdminService, mapGraphAdminService))
                .setControllerAdvice(new ApiExceptionHandler())
                .build();
    }

    @Test
    void createsStoreWithCreatedStatusAndLocation() throws Exception {
        UUID storeId = UUID.randomUUID();
        when(storeAdminService.create(any())).thenReturn(
                new StoreAdminResponse(storeId, "Store", "STORE-1", "Address", "City", "SP", true));

        storeMockMvc.perform(post("/api/v1/admin/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Store","code":"STORE-1","address":"Address","city":"City","state":"SP"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/v1/admin/stores/" + storeId))
                .andExpect(jsonPath("$.id").value(storeId.toString()))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void rejectsInvalidStoreRequestWithBadRequest() throws Exception {
        storeMockMvc.perform(post("/api/v1/admin/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","code":"","address":"","city":"","state":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void returnsConflictForAnAdministrativeUniquenessConflict() throws Exception {
        when(storeAdminService.create(any())).thenThrow(new AdminConflictException("Store code is already in use"));

        storeMockMvc.perform(post("/api/v1/admin/stores")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Store","code":"STORE-1","address":"Address","city":"City","state":"SP"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ADMIN_CONFLICT"));
    }

    @Test
    void updatesMapAndReturnsUpdatedDto() throws Exception {
        UUID storeId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        when(storeMapAdminService.update(any(), any(), any())).thenReturn(
                new StoreMapAdminResponse(
                        mapId, storeId, 1, "Main map", new BigDecimal("100"), new BigDecimal("80"),
                        BigDecimal.ONE, MapStatus.ACTIVE));

        storeMockMvc.perform(put("/api/v1/admin/stores/{storeId}/maps/{mapId}", storeId, mapId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":1,"name":"Main map","width":100,"height":80,"scaleMetersPerUnit":1,"status":"ACTIVE"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(mapId.toString()))
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void createsMapEdgeWithCreatedStatus() throws Exception {
        UUID mapId = UUID.randomUUID();
        UUID edgeId = UUID.randomUUID();
        UUID fromNodeId = UUID.randomUUID();
        UUID toNodeId = UUID.randomUUID();
        when(mapGraphAdminService.createEdge(any(), any())).thenReturn(
                new com.marketfastroute.admin.dto.MapEdgeAdminResponse(
                        edgeId, mapId, fromNodeId, toNodeId, BigDecimal.TEN, true, true));

        mapMockMvc.perform(post("/api/v1/admin/maps/{mapId}/edges", mapId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"fromNodeId":"%s","toNodeId":"%s","distanceMeters":10,"bidirectional":true}
                                """.formatted(fromNodeId, toNodeId)))
                .andExpect(status().isCreated())
                .andExpect(header().string(
                        "Location", "/api/v1/admin/maps/" + mapId + "/edges/" + edgeId))
                .andExpect(jsonPath("$.fromNodeId").value(fromNodeId.toString()))
                .andExpect(jsonPath("$.toNodeId").value(toNodeId.toString()));
    }

    @Test
    void rejectsInvalidNodePathWithBadRequest() throws Exception {
        mapMockMvc.perform(put("/api/v1/admin/maps/not-a-uuid/nodes/{nodeId}", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
    }
}
