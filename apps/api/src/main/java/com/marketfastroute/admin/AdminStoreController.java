package com.marketfastroute.admin;

import com.marketfastroute.admin.dto.CreateStoreMapRequest;
import com.marketfastroute.admin.dto.CreateStoreRequest;
import com.marketfastroute.admin.dto.StoreAdminResponse;
import com.marketfastroute.admin.dto.StoreMapAdminResponse;
import com.marketfastroute.admin.dto.UpdateStoreMapRequest;
import com.marketfastroute.admin.dto.UpdateStoreRequest;
import com.marketfastroute.store.StoreAdminService;
import com.marketfastroute.store.StoreMapAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminStoreController {

    private final StoreAdminService storeAdminService;
    private final StoreMapAdminService storeMapAdminService;

    public AdminStoreController(StoreAdminService storeAdminService, StoreMapAdminService storeMapAdminService) {
        this.storeAdminService = storeAdminService;
        this.storeMapAdminService = storeMapAdminService;
    }

    @GetMapping("/stores")
    public List<StoreAdminResponse> findStores() {
        return storeAdminService.findAll();
    }

    @GetMapping("/stores/{storeId}")
    public StoreAdminResponse findStore(@PathVariable UUID storeId) {
        return storeAdminService.findById(storeId);
    }

    @PostMapping("/stores")
    public ResponseEntity<StoreAdminResponse> createStore(@Valid @RequestBody CreateStoreRequest request) {
        StoreAdminResponse response = storeAdminService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/stores/" + response.id())).body(response);
    }

    @PutMapping("/stores/{storeId}")
    public StoreAdminResponse updateStore(
            @PathVariable UUID storeId,
            @Valid @RequestBody UpdateStoreRequest request
    ) {
        return storeAdminService.update(storeId, request);
    }

    @GetMapping("/stores/{storeId}/maps")
    public List<StoreMapAdminResponse> findMaps(@PathVariable UUID storeId) {
        return storeMapAdminService.findByStore(storeId);
    }

    @GetMapping("/stores/{storeId}/maps/{mapId}")
    public StoreMapAdminResponse findMap(@PathVariable UUID storeId, @PathVariable UUID mapId) {
        return storeMapAdminService.findById(storeId, mapId);
    }

    @PostMapping("/stores/{storeId}/maps")
    public ResponseEntity<StoreMapAdminResponse> createMap(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateStoreMapRequest request
    ) {
        StoreMapAdminResponse response = storeMapAdminService.create(storeId, request);
        return ResponseEntity.created(URI.create("/api/v1/admin/stores/" + storeId + "/maps/" + response.id()))
                .body(response);
    }

    @PutMapping("/stores/{storeId}/maps/{mapId}")
    public StoreMapAdminResponse updateMap(
            @PathVariable UUID storeId,
            @PathVariable UUID mapId,
            @Valid @RequestBody UpdateStoreMapRequest request
    ) {
        return storeMapAdminService.update(storeId, mapId, request);
    }
}
