package com.marketfastroute.store;

import com.marketfastroute.admin.AdminConflictException;
import com.marketfastroute.admin.dto.CreateStoreRequest;
import com.marketfastroute.admin.dto.UpdateStoreRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StoreAdminServiceTest {

    @Mock
    private StoreRepository storeRepository;

    private StoreAdminService service;

    @BeforeEach
    void setUp() {
        service = new StoreAdminService(storeRepository);
    }

    @Test
    void createsStoreWithLogicalActivationEnabled() {
        when(storeRepository.existsByCode("STORE-1")).thenReturn(false);
        when(storeRepository.save(any())).thenAnswer(invocation -> {
            Store store = invocation.getArgument(0);
            store.setId(UUID.randomUUID());
            return store;
        });

        var response = service.create(new CreateStoreRequest(
                "Store", "STORE-1", "Address", "City", "SP"));

        assertEquals("STORE-1", response.code());
        org.junit.jupiter.api.Assertions.assertTrue(response.active());
    }

    @Test
    void rejectsChangingToAnExistingStoreCode() {
        UUID storeId = UUID.randomUUID();
        Store store = new Store();
        store.setId(storeId);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(storeRepository.existsByCodeAndIdNot("STORE-2", storeId)).thenReturn(true);

        assertThrows(AdminConflictException.class, () -> service.update(
                storeId,
                new UpdateStoreRequest("Store", "STORE-2", "Address", "City", "SP", false)));
    }

    @Test
    void findsAndUpdatesAnInactiveStoreForAdministration() {
        UUID storeId = UUID.randomUUID();
        Store store = new Store();
        store.setId(storeId);
        store.setActive(false);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(storeRepository.existsByCodeAndIdNot("STORE-1", storeId)).thenReturn(false);
        when(storeRepository.save(store)).thenReturn(store);

        var response = service.update(storeId,
                new UpdateStoreRequest("Store", "STORE-1", "Address", "City", "SP", true));

        assertTrue(response.active());
        assertTrue(store.isActive());
    }
}
