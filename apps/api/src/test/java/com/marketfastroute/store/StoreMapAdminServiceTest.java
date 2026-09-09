package com.marketfastroute.store;

import com.marketfastroute.admin.AdminConflictException;
import com.marketfastroute.admin.dto.UpdateStoreMapRequest;
import com.marketfastroute.admin.dto.CreateStoreMapRequest;
import com.marketfastroute.map.MapStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreMapAdminServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreMapRepository storeMapRepository;

    private StoreMapAdminService service;

    @BeforeEach
    void setUp() {
        service = new StoreMapAdminService(storeRepository, storeMapRepository);
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
        when(storeMapRepository.findByStore_IdAndId(storeId, draftMapId)).thenReturn(Optional.of(draft));
        when(storeMapRepository.existsByStore_IdAndVersionAndIdNot(storeId, 2, draftMapId)).thenReturn(false);
        when(storeMapRepository.findActiveByStoreId(storeId)).thenReturn(Optional.of(active));

        assertThrows(AdminConflictException.class, () -> service.update(
                storeId,
                draftMapId,
                new UpdateStoreMapRequest(
                        2, "Map 2", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, MapStatus.ACTIVE)));
    }

    @Test
    void allowsActivatingTheOnlyMap() {
        UUID storeId = UUID.randomUUID();
        Store store = new Store();
        store.setId(storeId);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(storeMapRepository.existsByStore_IdAndVersion(storeId, 1)).thenReturn(false);
        when(storeMapRepository.findActiveByStoreId(storeId)).thenReturn(Optional.empty());
        when(storeMapRepository.save(any())).thenAnswer(invocation -> {
            StoreMap map = invocation.getArgument(0);
            map.setId(UUID.randomUUID());
            return map;
        });

        var response = service.create(storeId, new CreateStoreMapRequest(
                1, "Map 1", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, MapStatus.ACTIVE));

        org.junit.jupiter.api.Assertions.assertEquals(MapStatus.ACTIVE, response.status());
    }
}
