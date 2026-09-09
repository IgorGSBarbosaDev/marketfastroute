package com.marketfastroute.product;

import com.marketfastroute.map.Aisle;
import com.marketfastroute.map.MapNode;
import com.marketfastroute.map.Sector;
import com.marketfastroute.map.ShelfBlock;
import com.marketfastroute.store.Store;
import com.marketfastroute.store.StoreMap;
import com.marketfastroute.store.StoreNotFoundException;
import com.marketfastroute.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductLocationServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreProductRepository storeProductRepository;

    @Mock
    private ProductLocationRepository productLocationRepository;

    private ProductLocationService productLocationService;

    @BeforeEach
    void setUp() {
        productLocationService = new ProductLocationService(
                storeRepository,
                storeProductRepository,
                productLocationRepository,
                new ProductLocationMapper()
        );
    }

    @Test
    void listsMultipleValidLocationsAsDtos() {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        StoreProduct storeProduct = storeProduct(storeId, productId, true, true);
        StoreMap storeMap = storeMap(storeId, mapId);
        ProductLocation primary = location(storeId, productId, mapId, storeProduct, storeMap, true);
        ProductLocation secondary = location(storeId, productId, mapId, storeProduct, storeMap, false);
        primary.setModule("M-01");
        primary.setSide(ProductLocationSide.CENTER);
        primary.setX(new BigDecimal("10.0000"));
        primary.setY(new BigDecimal("20.0000"));

        when(storeRepository.existsById(storeId)).thenReturn(true);
        when(storeProductRepository.findAvailableByStoreIdAndProductId(storeId, productId))
                .thenReturn(Optional.of(storeProduct));
        when(productLocationRepository.findActiveByStoreIdAndProductId(storeId, productId))
                .thenReturn(List.of(primary, secondary));

        List<ProductLocationResponse> result = productLocationService.findByProduct(storeId, productId);

        assertEquals(List.of(
                new ProductLocationResponse(
                        primary.getId(), productId, storeId, mapId, null, null, null,
                        ProductLocationSide.CENTER, "M-01", null,
                        new BigDecimal("10.0000"), new BigDecimal("20.0000"),
                        primary.getNavigationNodeId(), true),
                new ProductLocationResponse(
                        secondary.getId(), productId, storeId, mapId, null, null, null,
                        null, null, null, null, null, secondary.getNavigationNodeId(), false)
        ), result);
        verify(productLocationRepository).findActiveByStoreIdAndProductId(storeId, productId);
    }

    @Test
    void doesNotExposeAnInactiveLocationEvenIfRepositoryReturnsOne() {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        StoreProduct storeProduct = storeProduct(storeId, productId, true, true);
        ProductLocation inactive = location(storeId, productId, UUID.randomUUID(), storeProduct,
                storeMap(storeId, UUID.randomUUID()), false);
        inactive.setActive(false);

        when(storeRepository.existsById(storeId)).thenReturn(true);
        when(storeProductRepository.findAvailableByStoreIdAndProductId(storeId, productId))
                .thenReturn(Optional.of(storeProduct));
        when(productLocationRepository.findActiveByStoreIdAndProductId(storeId, productId))
                .thenReturn(List.of(inactive));

        assertTrue(productLocationService.findByProduct(storeId, productId).isEmpty());
    }

    @Test
    void findsTheOnlyValidPrimaryLocation() {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        StoreProduct storeProduct = storeProduct(storeId, productId, true, true);
        UUID mapId = UUID.randomUUID();
        ProductLocation primary = location(storeId, productId, mapId, storeProduct, storeMap(storeId, mapId), true);

        prepareAvailableProduct(storeId, productId, storeProduct);
        when(productLocationRepository.findActivePrimaryByStoreIdAndProductId(storeId, productId))
                .thenReturn(List.of(primary));

        assertEquals(primary.getId(), productLocationService.findPrimaryByProduct(storeId, productId).id());
    }

    @Test
    void returnsNotFoundWhenThereIsNoPrimaryLocation() {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        StoreProduct storeProduct = storeProduct(storeId, productId, true, true);

        prepareAvailableProduct(storeId, productId, storeProduct);
        when(productLocationRepository.findActivePrimaryByStoreIdAndProductId(storeId, productId))
                .thenReturn(List.of());

        assertThrows(
                ProductLocationNotFoundException.class,
                () -> productLocationService.findPrimaryByProduct(storeId, productId)
        );
    }

    @Test
    void rejectsMoreThanOnePrimaryLocationWithoutChoosingArbitrarily() {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        StoreProduct storeProduct = storeProduct(storeId, productId, true, true);
        ProductLocation first = location(storeId, productId, UUID.randomUUID(), storeProduct,
                storeMap(storeId, UUID.randomUUID()), true);
        ProductLocation second = location(storeId, productId, UUID.randomUUID(), storeProduct,
                storeMap(storeId, UUID.randomUUID()), true);

        prepareAvailableProduct(storeId, productId, storeProduct);
        when(productLocationRepository.findActivePrimaryByStoreIdAndProductId(storeId, productId))
                .thenReturn(List.of(first, second));

        assertThrows(
                ProductLocationConsistencyException.class,
                () -> productLocationService.findPrimaryByProduct(storeId, productId)
        );
    }

    @Test
    void findsAnActiveLocationByIdOnlyForTheRequestedProductAndStore() {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        StoreProduct storeProduct = storeProduct(storeId, productId, true, true);
        UUID mapId = UUID.randomUUID();
        ProductLocation location = location(storeId, productId, mapId, storeProduct, storeMap(storeId, mapId), false);

        prepareAvailableProduct(storeId, productId, storeProduct);
        when(productLocationRepository.findActiveByIdAndStoreIdAndProductId(
                location.getId(), storeId, productId)).thenReturn(Optional.of(location));

        assertEquals(location.getId(), productLocationService
                .findById(storeId, productId, location.getId()).id());
    }

    @Test
    void returnsNotFoundForAnInactiveOrUnknownLocation() {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        StoreProduct storeProduct = storeProduct(storeId, productId, true, true);

        prepareAvailableProduct(storeId, productId, storeProduct);
        UUID locationId = UUID.randomUUID();
        when(productLocationRepository.findActiveByIdAndStoreIdAndProductId(locationId, storeId, productId))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductLocationNotFoundException.class,
                () -> productLocationService.findById(storeId, productId, locationId)
        );
    }

    @Test
    void rejectsARequestForANonexistentStore() {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(storeRepository.existsById(storeId)).thenReturn(false);

        assertThrows(
                StoreNotFoundException.class,
                () -> productLocationService.findByProduct(storeId, productId)
        );

        verifyNoInteractions(storeProductRepository, productLocationRepository);
    }

    @Test
    void rejectsAProductThatIsNotAssociatedWithTheStore() {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(storeRepository.existsById(storeId)).thenReturn(true);
        when(storeProductRepository.findAvailableByStoreIdAndProductId(storeId, productId))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productLocationService.findByProduct(storeId, productId)
        );

        verifyNoInteractions(productLocationRepository);
    }

    @Test
    void rejectsAnInactiveStoreProduct() {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        StoreProduct storeProduct = storeProduct(storeId, productId, false, true);
        when(storeRepository.existsById(storeId)).thenReturn(true);
        when(storeProductRepository.findAvailableByStoreIdAndProductId(storeId, productId))
                .thenReturn(Optional.of(storeProduct));

        assertThrows(
                ProductNotFoundException.class,
                () -> productLocationService.findByProduct(storeId, productId)
        );

        verify(productLocationRepository, never()).findActiveByStoreIdAndProductId(storeId, productId);
    }

    @Test
    void rejectsAnInactiveProduct() {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        StoreProduct storeProduct = storeProduct(storeId, productId, true, false);
        when(storeRepository.existsById(storeId)).thenReturn(true);
        when(storeProductRepository.findAvailableByStoreIdAndProductId(storeId, productId))
                .thenReturn(Optional.of(storeProduct));

        assertThrows(
                ProductNotFoundException.class,
                () -> productLocationService.findByProduct(storeId, productId)
        );

        verify(productLocationRepository, never()).findActiveByStoreIdAndProductId(storeId, productId);
    }

    @Test
    void rejectsALocationWhoseMapBelongsToAnotherStore() {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID otherStoreId = UUID.randomUUID();
        StoreProduct storeProduct = storeProduct(storeId, productId, true, true);
        UUID mapId = UUID.randomUUID();
        ProductLocation location = location(storeId, productId, mapId, storeProduct,
                storeMap(otherStoreId, mapId), false);

        prepareAvailableProduct(storeId, productId, storeProduct);
        when(productLocationRepository.findActiveByStoreIdAndProductId(storeId, productId))
                .thenReturn(List.of(location));

        assertThrows(
                ProductLocationConsistencyException.class,
                () -> productLocationService.findByProduct(storeId, productId)
        );
    }

    @Test
    void rejectsAReferenceToAnotherMapForEachSupportedMapElement() {
        assertReferenceFromAnotherMapIsRejected(Reference.SECTOR);
        assertReferenceFromAnotherMapIsRejected(Reference.AISLE);
        assertReferenceFromAnotherMapIsRejected(Reference.SHELF_BLOCK);
        assertReferenceFromAnotherMapIsRejected(Reference.NODE);
    }

    @Test
    void rejectsAnIncompatibleSectorAisleAndShelfBlockHierarchy() {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        StoreProduct storeProduct = storeProduct(storeId, productId, true, true);
        StoreMap map = storeMap(storeId, mapId);
        ProductLocation location = location(storeId, productId, mapId, storeProduct, map, false);
        Sector sector = mapReference(Sector.class, map, "Sector");
        Aisle aisle = mapReference(Aisle.class, map, "Aisle");
        ShelfBlock shelfBlock = mapReference(ShelfBlock.class, map, "Shelf block");
        aisle.setSectorId(UUID.randomUUID());
        shelfBlock.setSectorId(sector.getId());
        shelfBlock.setAisleId(UUID.randomUUID());
        location.setSectorId(sector.getId());
        location.setSector(sector);
        location.setAisleId(aisle.getId());
        location.setAisle(aisle);
        location.setShelfBlockId(shelfBlock.getId());
        location.setShelfBlock(shelfBlock);
        lenient().when(aisle.getSectorId()).thenReturn(UUID.randomUUID());
        UUID sectorId = sector.getId();
        lenient().when(shelfBlock.getSectorId()).thenReturn(sectorId);
        lenient().when(shelfBlock.getAisleId()).thenReturn(UUID.randomUUID());

        prepareAvailableProduct(storeId, productId, storeProduct);
        when(productLocationRepository.findActiveByStoreIdAndProductId(storeId, productId))
                .thenReturn(List.of(location));

        assertThrows(
                ProductLocationConsistencyException.class,
                () -> productLocationService.findByProduct(storeId, productId)
        );
    }

    private void assertReferenceFromAnotherMapIsRejected(Reference reference) {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        UUID otherMapId = UUID.randomUUID();
        StoreProduct storeProduct = storeProduct(storeId, productId, true, true);
        StoreMap map = storeMap(storeId, mapId);
        StoreMap otherMap = storeMap(storeId, otherMapId);
        ProductLocation location = location(storeId, productId, mapId, storeProduct, map, false);

        switch (reference) {
            case SECTOR -> {
                Sector sector = mapReference(Sector.class, otherMap, "Other sector");
                location.setSectorId(sector.getId());
                location.setSector(sector);
            }
            case AISLE -> {
                Aisle aisle = mapReference(Aisle.class, otherMap, "Other aisle");
                location.setAisleId(aisle.getId());
                location.setAisle(aisle);
            }
            case SHELF_BLOCK -> {
                ShelfBlock shelfBlock = mapReference(ShelfBlock.class, otherMap, "Other shelf");
                location.setShelfBlockId(shelfBlock.getId());
                location.setShelfBlock(shelfBlock);
            }
            case NODE -> {
                MapNode node = mapReference(MapNode.class, otherMap, "Other node");
                location.setNavigationNodeId(node.getId());
                location.setNavigationNode(node);
            }
        }

        prepareAvailableProduct(storeId, productId, storeProduct);
        when(productLocationRepository.findActiveByStoreIdAndProductId(storeId, productId))
                .thenReturn(List.of(location));

        assertThrows(
                ProductLocationConsistencyException.class,
                () -> productLocationService.findByProduct(storeId, productId)
        );
    }

    private void prepareAvailableProduct(UUID storeId, UUID productId, StoreProduct storeProduct) {
        when(storeRepository.existsById(storeId)).thenReturn(true);
        when(storeProductRepository.findAvailableByStoreIdAndProductId(storeId, productId))
                .thenReturn(Optional.of(storeProduct));
    }

    private StoreProduct storeProduct(UUID storeId, UUID productId, boolean active, boolean productActive) {
        Store store = org.mockito.Mockito.mock(Store.class);
        lenient().when(store.getId()).thenReturn(storeId);
        Product product = new Product();
        product.setId(productId);
        product.setActive(productActive);
        StoreProduct storeProduct = new StoreProduct();
        storeProduct.setId(UUID.randomUUID());
        storeProduct.setStore(store);
        storeProduct.setProduct(product);
        storeProduct.setActive(active);
        return storeProduct;
    }

    private ProductLocation location(
            UUID storeId,
            UUID productId,
            UUID mapId,
            StoreProduct storeProduct,
            StoreMap storeMap,
            boolean primary
    ) {
        ProductLocation location = new ProductLocation();
        location.setId(UUID.randomUUID());
        location.setStoreId(storeId);
        location.setStoreProductId(storeProduct.getId());
        location.setStoreProduct(storeProduct);
        location.setMapId(mapId);
        location.setStoreMap(storeMap);
        MapNode node = mapReference(MapNode.class, storeMap, "Product access");
        location.setNavigationNodeId(node.getId());
        location.setNavigationNode(node);
        location.setPrimaryLocation(primary);
        return location;
    }

    private StoreMap storeMap(UUID storeId, UUID mapId) {
        Store store = org.mockito.Mockito.mock(Store.class);
        lenient().when(store.getId()).thenReturn(storeId);
        StoreMap storeMap = org.mockito.Mockito.mock(StoreMap.class);
        lenient().when(storeMap.getId()).thenReturn(mapId);
        lenient().when(storeMap.getStore()).thenReturn(store);
        return storeMap;
    }

    private <T> T mapReference(Class<T> type, StoreMap storeMap, String name) {
        T reference = org.mockito.Mockito.mock(type);
        UUID id = UUID.randomUUID();
        if (reference instanceof Sector sector) {
            lenient().when(sector.getId()).thenReturn(id);
            lenient().when(sector.getStoreMap()).thenReturn(storeMap);
            lenient().when(sector.getName()).thenReturn(name);
        } else if (reference instanceof Aisle aisle) {
            lenient().when(aisle.getId()).thenReturn(id);
            lenient().when(aisle.getStoreMap()).thenReturn(storeMap);
            lenient().when(aisle.getName()).thenReturn(name);
        } else if (reference instanceof ShelfBlock shelfBlock) {
            lenient().when(shelfBlock.getId()).thenReturn(id);
            lenient().when(shelfBlock.getStoreMap()).thenReturn(storeMap);
            lenient().when(shelfBlock.getName()).thenReturn(name);
        } else if (reference instanceof MapNode node) {
            lenient().when(node.getId()).thenReturn(id);
            lenient().when(node.getStoreMap()).thenReturn(storeMap);
        }
        return reference;
    }

    private enum Reference {
        SECTOR,
        AISLE,
        SHELF_BLOCK,
        NODE
    }
}
