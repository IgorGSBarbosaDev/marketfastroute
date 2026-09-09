package com.marketfastroute.product;

import com.marketfastroute.admin.AdminConflictException;
import com.marketfastroute.admin.AdminResourceNotFoundException;
import com.marketfastroute.admin.dto.CreateProductLocationRequest;
import com.marketfastroute.map.AisleRepository;
import com.marketfastroute.map.MapNode;
import com.marketfastroute.map.MapNodeRepository;
import com.marketfastroute.map.SectorRepository;
import com.marketfastroute.map.ShelfBlockRepository;
import com.marketfastroute.store.Store;
import com.marketfastroute.store.StoreMap;
import com.marketfastroute.store.StoreMapRepository;
import com.marketfastroute.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductLocationAdminServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreProductRepository storeProductRepository;

    @Mock
    private StoreMapRepository storeMapRepository;

    @Mock
    private ProductLocationRepository productLocationRepository;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private AisleRepository aisleRepository;

    @Mock
    private ShelfBlockRepository shelfBlockRepository;

    @Mock
    private MapNodeRepository mapNodeRepository;

    private ProductLocationAdminService service;

    @BeforeEach
    void setUp() {
        service = new ProductLocationAdminService(
                storeRepository,
                storeProductRepository,
                storeMapRepository,
                productLocationRepository,
                sectorRepository,
                aisleRepository,
                shelfBlockRepository,
                mapNodeRepository);
    }

    @Test
    void rejectsStoreProductThatDoesNotBelongToRequestedStore() {
        UUID storeId = UUID.randomUUID();
        UUID storeProductId = UUID.randomUUID();
        Store store = mock(Store.class);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(storeProductRepository.findByStore_IdAndId(storeId, storeProductId)).thenReturn(Optional.empty());

        assertThrows(AdminResourceNotFoundException.class, () -> service.create(
                storeId, request(storeProductId, UUID.randomUUID(), UUID.randomUUID())));
    }

    @Test
    void rejectsPrimaryLocationWhenAnotherPrimaryAlreadyExists() {
        UUID storeId = UUID.randomUUID();
        UUID storeProductId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        Store store = mock(Store.class);
        StoreProduct storeProduct = new StoreProduct();
        StoreMap map = mock(StoreMap.class);
        MapNode node = mock(MapNode.class);
        when(node.getId()).thenReturn(UUID.randomUUID());

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(storeProductRepository.findByStore_IdAndId(storeId, storeProductId)).thenReturn(Optional.of(storeProduct));
        when(storeMapRepository.findByStore_IdAndId(storeId, mapId)).thenReturn(Optional.of(map));
        when(mapNodeRepository.findByStoreMap_IdAndId(mapId, node.getId())).thenReturn(Optional.of(node));
        when(productLocationRepository.existsByStoreIdAndStoreProductIdAndMapIdAndPrimaryLocationTrue(
                storeId, storeProductId, mapId)).thenReturn(true);

        assertThrows(AdminConflictException.class, () -> service.create(
                storeId, request(storeProductId, mapId, node.getId())));
    }

    private CreateProductLocationRequest request(UUID storeProductId, UUID mapId, UUID nodeId) {
        return new CreateProductLocationRequest(
                storeProductId, mapId, null, null, null, null, null, null,
                BigDecimal.ZERO, BigDecimal.ZERO, nodeId, true, true);
    }
}
