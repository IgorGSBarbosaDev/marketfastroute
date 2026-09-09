package com.marketfastroute.store;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

    @Mock
    private StoreRepository storeRepository;

    private StoreService storeService;

    @BeforeEach
    void setUp() {
        storeService = new StoreService(storeRepository, new StoreMapper());
    }

    @Test
    void returnsMappedStores() {
        Store store = newStore("Store One", "STORE-1");
        when(storeRepository.findAll()).thenReturn(List.of(store));

        List<StoreResponse> result = storeService.findAll();

        assertEquals(List.of(new StoreResponse(
                store.getId(),
                "Store One",
                "STORE-1",
                "Address",
                "City",
                "SP",
                true
        )), result);
        verify(storeRepository).findAll();
    }

    @Test
    void returnsMappedStoreWhenItExists() {
        Store store = newStore("Store One", "STORE-1");
        when(storeRepository.findById(store.getId())).thenReturn(Optional.of(store));

        StoreResponse result = storeService.findById(store.getId());

        assertEquals(new StoreResponse(
                store.getId(),
                "Store One",
                "STORE-1",
                "Address",
                "City",
                "SP",
                true
        ), result);
        verify(storeRepository).findById(store.getId());
    }

    @Test
    void throwsStoreNotFoundWhenRequestedStoreDoesNotExist() {
        UUID storeId = UUID.randomUUID();
        when(storeRepository.findById(storeId)).thenReturn(Optional.empty());

        StoreNotFoundException exception = assertThrows(
                StoreNotFoundException.class,
                () -> storeService.findById(storeId)
        );

        assertEquals("Store not found: " + storeId, exception.getMessage());
        verify(storeRepository).findById(storeId);
    }

    private Store newStore(String name, String code) {
        Store store = new Store();
        store.setId(UUID.randomUUID());
        store.setName(name);
        store.setCode(code);
        store.setAddress("Address");
        store.setCity("City");
        store.setState("SP");
        return store;
    }
}
