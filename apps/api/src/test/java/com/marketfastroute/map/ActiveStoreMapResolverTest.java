package com.marketfastroute.map;

import com.marketfastroute.store.Store;
import com.marketfastroute.store.StoreMap;
import com.marketfastroute.store.StoreMapRepository;
import com.marketfastroute.store.StoreNotFoundException;
import com.marketfastroute.store.StoreRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class ActiveStoreMapResolverTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreMapRepository storeMapRepository;

    @Test
    void resolvesTheActiveMapForAnActiveStore() {
        UUID storeId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        Store store = store(storeId);
        StoreMap storeMap = activeMap(store, mapId);
        when(storeRepository.existsByIdAndActiveTrue(storeId)).thenReturn(true);
        when(storeMapRepository.findActiveByStoreId(storeId)).thenReturn(Optional.of(storeMap));

        StoreMap result = new ActiveStoreMapResolver(storeRepository, storeMapRepository).resolve(storeId);

        assertEquals(mapId, result.getId());
        verify(storeRepository).existsByIdAndActiveTrue(storeId);
        verify(storeMapRepository).findActiveByStoreId(storeId);
    }

    @Test
    void rejectsAnInactiveStoreBeforeLookingForItsMap() {
        UUID storeId = UUID.randomUUID();
        when(storeRepository.existsByIdAndActiveTrue(storeId)).thenReturn(false);

        assertThrows(
                StoreNotFoundException.class,
                () -> new ActiveStoreMapResolver(storeRepository, storeMapRepository).resolve(storeId)
        );

        verifyNoInteractions(storeMapRepository);
    }

    @Test
    void rejectsAStoreWithoutAnActiveMap() {
        UUID storeId = UUID.randomUUID();
        when(storeRepository.existsByIdAndActiveTrue(storeId)).thenReturn(true);
        when(storeMapRepository.findActiveByStoreId(storeId)).thenReturn(Optional.empty());

        assertThrows(
                StoreMapNotFoundException.class,
                () -> new ActiveStoreMapResolver(storeRepository, storeMapRepository).resolve(storeId)
        );
    }

    @Test
    void rejectsAnActiveMapThatBelongsToAnotherStore() {
        UUID storeId = UUID.randomUUID();
        StoreMap storeMap = mock(StoreMap.class);
        Store foreignStore = store(UUID.randomUUID());
        when(storeMap.getStatus()).thenReturn(MapStatus.ACTIVE);
        when(storeMap.getStore()).thenReturn(foreignStore);
        when(storeRepository.existsByIdAndActiveTrue(storeId)).thenReturn(true);
        when(storeMapRepository.findActiveByStoreId(storeId)).thenReturn(Optional.of(storeMap));

        assertThrows(
                MapConsistencyException.class,
                () -> new ActiveStoreMapResolver(storeRepository, storeMapRepository).resolve(storeId)
        );
    }

    private Store store(UUID storeId) {
        Store store = mock(Store.class);
        when(store.getId()).thenReturn(storeId);
        return store;
    }

    private StoreMap activeMap(Store store, UUID mapId) {
        StoreMap storeMap = mock(StoreMap.class);
        when(storeMap.getId()).thenReturn(mapId);
        when(storeMap.getStore()).thenReturn(store);
        when(storeMap.getStatus()).thenReturn(MapStatus.ACTIVE);
        return storeMap;
    }
}
