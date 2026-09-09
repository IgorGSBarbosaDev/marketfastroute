package com.marketfastroute.store;

import com.marketfastroute.admin.AdminConflictException;
import com.marketfastroute.admin.AdminResourceNotFoundException;
import com.marketfastroute.admin.dto.CreateStoreMapRequest;
import com.marketfastroute.admin.dto.StoreMapAdminResponse;
import com.marketfastroute.admin.dto.UpdateStoreMapRequest;
import com.marketfastroute.map.MapStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StoreMapAdminService {

    private final StoreRepository storeRepository;
    private final StoreMapRepository storeMapRepository;

    public StoreMapAdminService(StoreRepository storeRepository, StoreMapRepository storeMapRepository) {
        this.storeRepository = storeRepository;
        this.storeMapRepository = storeMapRepository;
    }

    public List<StoreMapAdminResponse> findByStore(UUID storeId) {
        ensureStore(storeId);
        return storeMapRepository.findByStore_IdOrderByVersion(storeId).stream()
                .map(this::toResponse)
                .toList();
    }

    public StoreMapAdminResponse findById(UUID storeId, UUID mapId) {
        ensureStoreMap(storeId, mapId);
        return toResponse(storeMapRepository.findByStore_IdAndId(storeId, mapId).orElseThrow());
    }

    @Transactional
    public StoreMapAdminResponse create(UUID storeId, CreateStoreMapRequest request) {
        Store store = ensureStore(storeId);
        ensureVersionIsAvailable(storeId, request.version(), null);
        ensureCanBecomeActive(storeId, null, request.status() == null ? MapStatus.DRAFT : request.status());

        StoreMap map = new StoreMap();
        map.setStore(store);
        apply(map, request);
        return toResponse(storeMapRepository.save(map));
    }

    @Transactional
    public StoreMapAdminResponse update(UUID storeId, UUID mapId, UpdateStoreMapRequest request) {
        StoreMap map = ensureStoreMap(storeId, mapId);
        ensureVersionIsAvailable(storeId, request.version(), mapId);
        ensureCanBecomeActive(storeId, mapId, request.status());

        map.setVersion(request.version());
        map.setName(request.name());
        map.setWidth(request.width());
        map.setHeight(request.height());
        map.setScaleMetersPerUnit(request.scaleMetersPerUnit());
        map.setStatus(request.status());
        return toResponse(storeMapRepository.save(map));
    }

    private Store ensureStore(UUID storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
    }

    private StoreMap ensureStoreMap(UUID storeId, UUID mapId) {
        ensureStore(storeId);
        return storeMapRepository.findByStore_IdAndId(storeId, mapId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Store map"));
    }

    private void ensureVersionIsAvailable(UUID storeId, int version, UUID mapId) {
        boolean exists = mapId == null
                ? storeMapRepository.existsByStore_IdAndVersion(storeId, version)
                : storeMapRepository.existsByStore_IdAndVersionAndIdNot(storeId, version, mapId);
        if (exists) {
            throw new AdminConflictException("Map version is already in use for the store");
        }
    }

    private void ensureCanBecomeActive(UUID storeId, UUID mapId, MapStatus status) {
        if (status != MapStatus.ACTIVE) {
            return;
        }
        storeMapRepository.findActiveByStoreId(storeId)
                .filter(activeMap -> !Objects.equals(activeMap.getId(), mapId))
                .ifPresent(activeMap -> {
                    throw new AdminConflictException("The store already has an active map");
                });
    }

    private void apply(StoreMap map, CreateStoreMapRequest request) {
        map.setVersion(request.version());
        map.setName(request.name());
        map.setWidth(request.width());
        map.setHeight(request.height());
        map.setScaleMetersPerUnit(request.scaleMetersPerUnit());
        map.setStatus(request.status() == null ? MapStatus.DRAFT : request.status());
    }

    private StoreMapAdminResponse toResponse(StoreMap map) {
        return new StoreMapAdminResponse(
                map.getId(),
                map.getStore().getId(),
                map.getVersion(),
                map.getName(),
                map.getWidth(),
                map.getHeight(),
                map.getScaleMetersPerUnit(),
                map.getStatus()
        );
    }
}
