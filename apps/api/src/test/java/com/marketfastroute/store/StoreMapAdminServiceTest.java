package com.marketfastroute.store;

import com.marketfastroute.admin.AdminConflictException;
import com.marketfastroute.admin.AdminValidationException;
import com.marketfastroute.admin.dto.UpdateStoreMapRequest;
import com.marketfastroute.admin.dto.CreateStoreMapRequest;
import com.marketfastroute.map.MapStatus;
import com.marketfastroute.map.MapPublicationValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreMapAdminServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreMapRepository storeMapRepository;

    @Mock
    private MapPublicationValidator mapPublicationValidator;

    private StoreMapAdminService service;

    @BeforeEach
    void setUp() {
        service = new StoreMapAdminService(storeRepository, storeMapRepository, mapPublicationValidator);
    }

    @Test
    void rejectsActivationWhenAnotherMapIsAlreadyActive() {
        UUID storeId = UUID.randomUUID();
        UUID currentMapId = UUID.randomUUID();
        UUID draftMapId = UUID.randomUUID();
        Store store = new Store();
        store.setId(storeId);
        StoreMap draft = new StoreMap();
        draft.setId(draftMapId);
        draft.setStore(store);
        StoreMap active = new StoreMap();
        active.setId(currentMapId);
        active.setStore(store);

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(storeRepository.findByIdForUpdate(storeId)).thenReturn(Optional.of(store));
        when(storeMapRepository.findByStore_IdAndIdForUpdate(storeId, draftMapId)).thenReturn(Optional.of(draft));
        when(storeMapRepository.existsByStore_IdAndVersionAndIdNot(storeId, 2, draftMapId)).thenReturn(false);
        when(storeMapRepository.findActiveByStoreId(storeId)).thenReturn(Optional.of(active));

        assertThrows(AdminConflictException.class, () -> service.update(
                storeId,
                draftMapId,
                new UpdateStoreMapRequest(
                        2, "Map 2", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, MapStatus.ACTIVE)));
        verify(storeRepository).findByIdForUpdate(storeId);
    }

    @Test
    void rejectsCreatingAMapOutsideDraftBeforeItCanBeConfigured() {
        UUID storeId = UUID.randomUUID();
        Store store = new Store();
        store.setId(storeId);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(storeMapRepository.existsByStore_IdAndVersion(storeId, 1)).thenReturn(false);

        for (MapStatus status : List.of(MapStatus.ACTIVE, MapStatus.ARCHIVED)) {
            assertThrows(com.marketfastroute.admin.AdminValidationException.class, () -> service.create(
                    storeId,
                    new CreateStoreMapRequest(
                            1, "Map 1", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, status)));
        }
    }

    @Test
    void rejectsSkippingTheExplicitArchiveStepForAnActiveMap() {
        UUID storeId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        Store store = new Store();
        store.setId(storeId);
        StoreMap active = new StoreMap();
        active.setId(mapId);
        active.setStore(store);
        active.setVersion(1);
        active.setName("Map 1");
        active.setWidth(BigDecimal.TEN);
        active.setHeight(BigDecimal.TEN);
        active.setScaleMetersPerUnit(BigDecimal.ONE);
        active.setStatus(MapStatus.ACTIVE);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(storeMapRepository.findByStore_IdAndIdForUpdate(storeId, mapId)).thenReturn(Optional.of(active));
        when(storeMapRepository.existsByStore_IdAndVersionAndIdNot(storeId, 1, mapId)).thenReturn(false);

        assertThrows(AdminValidationException.class, () -> service.update(
                storeId,
                mapId,
                new UpdateStoreMapRequest(
                        1, "Map 1", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, MapStatus.DRAFT)));
    }

    @Test
    void rejectsReactivatingAnArchivedMap() {
        UUID storeId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        Store store = new Store();
        store.setId(storeId);
        StoreMap archived = new StoreMap();
        archived.setId(mapId);
        archived.setStore(store);
        archived.setStatus(MapStatus.ARCHIVED);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(storeRepository.findByIdForUpdate(storeId)).thenReturn(Optional.of(store));
        when(storeMapRepository.findByStore_IdAndIdForUpdate(storeId, mapId)).thenReturn(Optional.of(archived));
        when(storeMapRepository.existsByStore_IdAndVersionAndIdNot(storeId, 1, mapId)).thenReturn(false);

        assertThrows(AdminValidationException.class, () -> service.update(
                storeId,
                mapId,
                new UpdateStoreMapRequest(
                        1, "Map 1", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, MapStatus.ACTIVE)));
    }

    @Test
    void createsAMapAsDraftWhenStatusIsOmitted() {
        UUID storeId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        Store store = new Store();
        store.setId(storeId);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(storeMapRepository.existsByStore_IdAndVersion(storeId, 1)).thenReturn(false);
        when(storeMapRepository.save(any())).thenAnswer(invocation -> {
            StoreMap map = invocation.getArgument(0);
            map.setId(mapId);
            return map;
        });

        var response = service.create(storeId, new CreateStoreMapRequest(
                1, "Map 1", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, null));

        assertEquals(MapStatus.DRAFT, response.status());
    }

    @Test
    void allowsActivatingAConfiguredMapWhenNoOtherMapIsActive() {
        UUID storeId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        Store store = new Store();
        store.setId(storeId);
        StoreMap draft = new StoreMap();
        draft.setId(mapId);
        draft.setStore(store);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(storeRepository.findByIdForUpdate(storeId)).thenReturn(Optional.of(store));
        when(storeMapRepository.findByStore_IdAndIdForUpdate(storeId, mapId)).thenReturn(Optional.of(draft));
        when(storeMapRepository.existsByStore_IdAndVersionAndIdNot(storeId, 1, mapId)).thenReturn(false);
        when(storeMapRepository.findActiveByStoreId(storeId)).thenReturn(Optional.empty());
        when(storeMapRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.update(storeId, mapId, new UpdateStoreMapRequest(
                1, "Map 1", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, MapStatus.ACTIVE));

        org.junit.jupiter.api.Assertions.assertEquals(MapStatus.ACTIVE, response.status());
        verify(storeRepository).findByIdForUpdate(storeId);
    }

    @Test
    void allowsExplicitlyArchivingAnActiveMap() {
        UUID storeId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        Store store = new Store();
        store.setId(storeId);
        StoreMap active = new StoreMap();
        active.setId(mapId);
        active.setStore(store);
        active.setVersion(1);
        active.setName("Map 1");
        active.setWidth(BigDecimal.TEN);
        active.setHeight(BigDecimal.TEN);
        active.setScaleMetersPerUnit(BigDecimal.ONE);
        active.setStatus(MapStatus.ACTIVE);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(storeMapRepository.findByStore_IdAndIdForUpdate(storeId, mapId)).thenReturn(Optional.of(active));
        when(storeMapRepository.existsByStore_IdAndVersionAndIdNot(storeId, 1, mapId)).thenReturn(false);
        when(storeMapRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.update(storeId, mapId, new UpdateStoreMapRequest(
                1, "Map 1", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, MapStatus.ARCHIVED));

        assertEquals(MapStatus.ARCHIVED, response.status());
    }

    @Test
    void rejectsChangingPublishedMapMetadataWhileArchiving() {
        UUID storeId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        Store store = new Store();
        store.setId(storeId);
        StoreMap active = new StoreMap();
        active.setId(mapId);
        active.setStore(store);
        active.setVersion(1);
        active.setName("Map 1");
        active.setWidth(new BigDecimal("100"));
        active.setHeight(new BigDecimal("80"));
        active.setScaleMetersPerUnit(BigDecimal.ONE);
        active.setStatus(MapStatus.ACTIVE);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(storeMapRepository.findByStore_IdAndIdForUpdate(storeId, mapId)).thenReturn(Optional.of(active));
        when(storeMapRepository.existsByStore_IdAndVersionAndIdNot(storeId, 1, mapId)).thenReturn(false);

        assertThrows(AdminValidationException.class, () -> service.update(
                storeId,
                mapId,
                new UpdateStoreMapRequest(
                        1, "Map 1", new BigDecimal("101"), new BigDecimal("80"), BigDecimal.ONE,
                        MapStatus.ARCHIVED)));
    }
}
