package com.marketfastroute.store;

import com.marketfastroute.admin.AdminConflictException;
import com.marketfastroute.admin.AdminResourceNotFoundException;
import com.marketfastroute.admin.dto.CreateStoreMapRequest;
import com.marketfastroute.admin.dto.StoreMapAdminResponse;
import com.marketfastroute.admin.dto.UpdateStoreMapRequest;
import com.marketfastroute.map.MapStatus;
import com.marketfastroute.map.MapPublicationValidator;
import com.marketfastroute.admin.AdminValidationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StoreMapAdminService {

    private final StoreRepository storeRepository;
    private final StoreMapRepository storeMapRepository;
    private final MapPublicationValidator mapPublicationValidator;

    public StoreMapAdminService(
            StoreRepository storeRepository,
            StoreMapRepository storeMapRepository,
            MapPublicationValidator mapPublicationValidator
    ) {
        this.storeRepository = storeRepository;
        this.storeMapRepository = storeMapRepository;
        this.mapPublicationValidator = mapPublicationValidator;
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
        MapStatus status = request.status() == null ? MapStatus.DRAFT : request.status();
        if (status != MapStatus.DRAFT) {
            throw new AdminValidationException("Create the map as DRAFT, configure it, then validate before activation");
        }

        StoreMap map = new StoreMap();
        map.setStore(store);
        apply(map, request);
        return toResponse(storeMapRepository.save(map));
    }

    @Transactional
    public StoreMapAdminResponse update(UUID storeId, UUID mapId, UpdateStoreMapRequest request) {
        ensureStore(storeId);
        if (request.status() == MapStatus.ACTIVE) {
            storeRepository.findByIdForUpdate(storeId)
                    .orElseThrow(() -> new StoreNotFoundException(storeId));
        }
        StoreMap map = storeMapRepository.findByStore_IdAndIdForUpdate(storeId, mapId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Store map"));
        ensureVersionIsAvailable(storeId, request.version(), mapId);
        ensureStatusTransitionIsAllowed(map.getStatus(), request.status());
        ensurePublishedMetadataIsUnchanged(map, request);
        ensureCanBecomeActive(storeId, mapId, request.status());

        map.setVersion(request.version());
        map.setName(request.name());
        map.setWidth(request.width());
        map.setHeight(request.height());
        map.setScaleMetersPerUnit(request.scaleMetersPerUnit());
        map.setStatus(request.status());
        if (request.status() == MapStatus.ACTIVE) {
            mapPublicationValidator.requirePublishable(map);
        }
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

    private void ensureStatusTransitionIsAllowed(MapStatus currentStatus, MapStatus nextStatus) {
        boolean sameStatus = currentStatus == nextStatus;
        boolean publishDraft = currentStatus == MapStatus.DRAFT && nextStatus == MapStatus.ACTIVE;
        boolean archiveActive = currentStatus == MapStatus.ACTIVE && nextStatus == MapStatus.ARCHIVED;
        if (!sameStatus && !publishDraft && !archiveActive) {
            throw new AdminValidationException(
                    "A map must move from DRAFT to ACTIVE and from ACTIVE to ARCHIVED");
        }
    }

    private void ensurePublishedMetadataIsUnchanged(StoreMap map, UpdateStoreMapRequest request) {
        if (map.getStatus() == MapStatus.DRAFT) {
            return;
        }
        boolean unchanged = map.getVersion() == request.version()
                && Objects.equals(map.getName(), request.name())
                && sameDecimal(map.getWidth(), request.width())
                && sameDecimal(map.getHeight(), request.height())
                && sameDecimal(map.getScaleMetersPerUnit(), request.scaleMetersPerUnit());
        if (!unchanged) {
            throw new AdminValidationException("Published map versions are immutable; create a new draft version");
        }
    }

    private boolean sameDecimal(BigDecimal current, BigDecimal requested) {
        return current == null ? requested == null : requested != null && current.compareTo(requested) == 0;
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
