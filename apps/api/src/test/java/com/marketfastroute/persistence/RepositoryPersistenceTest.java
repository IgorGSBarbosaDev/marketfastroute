package com.marketfastroute.persistence;

import com.marketfastroute.map.Aisle;
import com.marketfastroute.map.MapEdge;
import com.marketfastroute.map.MapEdgeRepository;
import com.marketfastroute.map.MapNode;
import com.marketfastroute.map.MapNodeRepository;
import com.marketfastroute.map.MapNodeType;
import com.marketfastroute.map.MapStatus;
import com.marketfastroute.map.Sector;
import com.marketfastroute.map.ShelfBlock;
import com.marketfastroute.product.Category;
import com.marketfastroute.product.CategoryRepository;
import com.marketfastroute.product.Product;
import com.marketfastroute.product.ProductLocation;
import com.marketfastroute.product.ProductLocationRepository;
import com.marketfastroute.product.ProductLocationSide;
import com.marketfastroute.product.ProductRepository;
import com.marketfastroute.product.StoreProduct;
import com.marketfastroute.product.StoreProductRepository;
import com.marketfastroute.store.Store;
import com.marketfastroute.store.StoreMap;
import com.marketfastroute.store.StoreMapRepository;
import com.marketfastroute.store.StoreRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Constructor;
import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class RepositoryPersistenceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private StoreRepository storeRepository;

    @Autowired
    private StoreMapRepository storeMapRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private StoreProductRepository storeProductRepository;

    @Autowired
    private ProductLocationRepository productLocationRepository;

    @Autowired
    private MapNodeRepository mapNodeRepository;

    @Autowired
    private MapEdgeRepository mapEdgeRepository;

    @Test
    void persistsCatalogAndAvailabilityAndQueriesEachRepository() {
        Store store = storeRepository.saveAndFlush(newStore("STORE-1"));
        Category category = categoryRepository.saveAndFlush(newCategory("CATEGORY-1"));
        Product product = productRepository.saveAndFlush(
                newProduct(category, "SKU-1", "7890000000001"));
        StoreProduct storeProduct = storeProductRepository.saveAndFlush(
                newStoreProduct(store, product));

        entityManager.clear();

        Store persistedStore = storeRepository.findById(store.getId()).orElseThrow();
        Category persistedCategory = categoryRepository.findById(category.getId()).orElseThrow();
        Product persistedProduct = productRepository.findById(product.getId()).orElseThrow();
        StoreProduct persistedStoreProduct = storeProductRepository.findById(storeProduct.getId()).orElseThrow();

        assertEquals("STORE-1", persistedStore.getCode());
        assertEquals("CATEGORY-1", persistedCategory.getCode());
        assertEquals("SKU-1", persistedProduct.getSku());
        assertEquals(category.getId(), persistedProduct.getCategory().getId());
        assertEquals(store.getId(), persistedStoreProduct.getStore().getId());
        assertEquals(product.getId(), persistedStoreProduct.getProduct().getId());
        assertEquals(1, storeRepository.findAll().size());
        assertEquals(1, categoryRepository.findAll().size());
        assertEquals(1, productRepository.findAll().size());
        assertEquals(1, storeProductRepository.findAll().size());
    }

    @Test
    void persistsMapGraphAndProductLocationRelationshipsAndEnums() {
        Store store = storeRepository.saveAndFlush(newStore("STORE-2"));
        Category category = categoryRepository.saveAndFlush(newCategory("CATEGORY-2"));
        Product product = productRepository.saveAndFlush(
                newProduct(category, "SKU-2", "7890000000002"));
        StoreProduct storeProduct = storeProductRepository.saveAndFlush(
                newStoreProduct(store, product));
        StoreMap storeMap = storeMapRepository.saveAndFlush(
                newStoreMap(store, 1, MapStatus.ACTIVE));

        Sector sector = persistSector(storeMap, "SECTOR-1");
        Aisle aisle = persistAisle(storeMap, sector, "AISLE-1");
        ShelfBlock shelfBlock = persistShelfBlock(storeMap, sector, aisle, "SHELF-1");
        MapNode fromNode = persistNode(storeMap, MapNodeType.ENTRANCE, "Entrance");
        MapNode toNode = persistNode(storeMap, MapNodeType.PRODUCT_ACCESS, "Product access");

        MapEdge edge = newEntity(MapEdge.class);
        edge.setStoreMap(storeMap);
        edge.setFromNodeId(fromNode.getId());
        edge.setToNodeId(toNode.getId());
        edge.setDistanceMeters(new BigDecimal("12.5000"));
        edge.setBidirectional(true);
        edge = mapEdgeRepository.saveAndFlush(edge);

        ProductLocation location = newEntity(ProductLocation.class);
        location.setStoreId(store.getId());
        location.setStoreProductId(storeProduct.getId());
        location.setMapId(storeMap.getId());
        location.setSectorId(sector.getId());
        location.setAisleId(aisle.getId());
        location.setShelfBlockId(shelfBlock.getId());
        location.setSide(ProductLocationSide.CENTER);
        location.setModule("M-01");
        location.setShelfLevel(2);
        location.setX(new BigDecimal("10.0000"));
        location.setY(new BigDecimal("20.0000"));
        location.setNavigationNodeId(toNode.getId());
        location.setPrimaryLocation(true);
        location = productLocationRepository.saveAndFlush(location);

        entityManager.clear();

        StoreMap persistedMap = storeMapRepository.findById(storeMap.getId()).orElseThrow();
        MapNode persistedNode = mapNodeRepository.findById(toNode.getId()).orElseThrow();
        MapEdge persistedEdge = mapEdgeRepository.findById(edge.getId()).orElseThrow();
        ProductLocation persistedLocation = productLocationRepository.findById(location.getId()).orElseThrow();

        assertEquals(MapStatus.ACTIVE, persistedMap.getStatus());
        assertEquals(MapNodeType.PRODUCT_ACCESS, persistedNode.getType());
        assertEquals(storeMap.getId(), persistedEdge.getStoreMap().getId());
        assertEquals(fromNode.getId(), persistedEdge.getFromNodeId());
        assertEquals(toNode.getId(), persistedEdge.getToNodeId());
        assertEquals(ProductLocationSide.CENTER, persistedLocation.getSide());
        assertEquals(storeProduct.getId(), persistedLocation.getStoreProductId());
        assertEquals(storeMap.getId(), persistedLocation.getStoreMap().getId());
        assertEquals(product.getId(), persistedLocation.getStoreProduct().getProduct().getId());
        assertEquals(toNode.getId(), persistedLocation.getNavigationNode().getId());
        assertEquals(shelfBlock.getId(), persistedLocation.getShelfBlock().getId());
    }

    @Test
    void rejectsProductLocationThatCombinesStoreProductAndMapFromDifferentStores() {
        Store firstStore = storeRepository.saveAndFlush(newStore("STORE-3"));
        Store secondStore = storeRepository.saveAndFlush(newStore("STORE-4"));
        Category category = categoryRepository.saveAndFlush(newCategory("CATEGORY-3"));
        Product product = productRepository.saveAndFlush(
                newProduct(category, "SKU-3", "7890000000003"));
        StoreProduct storeProduct = storeProductRepository.saveAndFlush(
                newStoreProduct(firstStore, product));
        StoreMap secondMap = storeMapRepository.saveAndFlush(
                newStoreMap(secondStore, 1, MapStatus.DRAFT));
        MapNode secondMapNode = persistNode(secondMap, MapNodeType.PRODUCT_ACCESS, "Other map");

        ProductLocation location = newEntity(ProductLocation.class);
        location.setStoreId(firstStore.getId());
        location.setStoreProductId(storeProduct.getId());
        location.setMapId(secondMap.getId());
        location.setNavigationNodeId(secondMapNode.getId());

        assertThrows(DataIntegrityViolationException.class,
                () -> productLocationRepository.saveAndFlush(location));
    }

    @Test
    void rejectsMapEdgeThatConnectsNodesFromDifferentMaps() {
        Store store = storeRepository.saveAndFlush(newStore("STORE-5"));
        StoreMap firstMap = storeMapRepository.saveAndFlush(
                newStoreMap(store, 1, MapStatus.DRAFT));
        StoreMap secondMap = storeMapRepository.saveAndFlush(
                newStoreMap(store, 2, MapStatus.DRAFT));
        MapNode firstNode = persistNode(firstMap, MapNodeType.PATH, "First map");
        MapNode secondNode = persistNode(secondMap, MapNodeType.PATH, "Second map");

        MapEdge edge = newEntity(MapEdge.class);
        edge.setStoreMap(firstMap);
        edge.setFromNodeId(firstNode.getId());
        edge.setToNodeId(secondNode.getId());
        edge.setDistanceMeters(BigDecimal.ONE);
        edge.setBidirectional(false);

        assertThrows(DataIntegrityViolationException.class,
                () -> mapEdgeRepository.saveAndFlush(edge));
    }

    @Test
    void enforcesUniqueStoreCodeThroughRepository() {
        storeRepository.saveAndFlush(newStore("STORE-6"));

        assertThrows(DataIntegrityViolationException.class,
                () -> storeRepository.saveAndFlush(newStore("STORE-6")));
    }

    @Test
    void enforcesUniqueStoreProductPerStoreAndProductThroughRepository() {
        Store store = storeRepository.saveAndFlush(newStore("STORE-7"));
        Category category = categoryRepository.saveAndFlush(newCategory("CATEGORY-7"));
        Product product = productRepository.saveAndFlush(
                newProduct(category, "SKU-7", "7890000000007"));
        storeProductRepository.saveAndFlush(newStoreProduct(store, product));

        assertThrows(DataIntegrityViolationException.class,
                () -> storeProductRepository.saveAndFlush(newStoreProduct(store, product)));
    }

    @Test
    void enforcesUniqueProductEanWhenPresentThroughRepository() {
        Category category = categoryRepository.saveAndFlush(newCategory("CATEGORY-12"));
        productRepository.saveAndFlush(newProduct(category, "SKU-12-A", "7890000000012"));

        assertThrows(DataIntegrityViolationException.class,
                () -> productRepository.saveAndFlush(
                        newProduct(category, "SKU-12-B", "7890000000012")));
    }

    @Test
    void enforcesUniqueMapVersionPerStoreThroughRepository() {
        Store store = storeRepository.saveAndFlush(newStore("STORE-12"));
        storeMapRepository.saveAndFlush(newStoreMap(store, 1, MapStatus.DRAFT));

        assertThrows(DataIntegrityViolationException.class,
                () -> storeMapRepository.saveAndFlush(newStoreMap(store, 1, MapStatus.ARCHIVED)));
    }

    @Test
    void rejectsNullRequiredStoreName() {
        Store store = newStore("STORE-8");
        store.setName(null);

        assertThrows(DataIntegrityViolationException.class,
                () -> storeRepository.saveAndFlush(store));
    }

    @Test
    void cascadesStoreProductDeleteToProductLocations() {
        Store store = storeRepository.saveAndFlush(newStore("STORE-9"));
        Category category = categoryRepository.saveAndFlush(newCategory("CATEGORY-9"));
        Product product = productRepository.saveAndFlush(
                newProduct(category, "SKU-9", "7890000000009"));
        StoreProduct storeProduct = storeProductRepository.saveAndFlush(
                newStoreProduct(store, product));
        StoreMap storeMap = storeMapRepository.saveAndFlush(
                newStoreMap(store, 1, MapStatus.DRAFT));
        MapNode node = persistNode(storeMap, MapNodeType.PRODUCT_ACCESS, "Product");
        ProductLocation location = newEntity(ProductLocation.class);
        location.setStoreId(store.getId());
        location.setStoreProductId(storeProduct.getId());
        location.setMapId(storeMap.getId());
        location.setNavigationNodeId(node.getId());
        location = productLocationRepository.saveAndFlush(location);

        entityManager.clear();
        storeProductRepository.deleteById(storeProduct.getId());
        storeProductRepository.flush();
        entityManager.clear();

        assertFalse(productLocationRepository.existsById(location.getId()));
        assertTrue(productRepository.existsById(product.getId()));
        assertTrue(storeMapRepository.existsById(storeMap.getId()));
    }

    @Test
    void cascadesStoreMapDeleteToNodesAndEdges() {
        Store store = storeRepository.saveAndFlush(newStore("STORE-10"));
        StoreMap storeMap = storeMapRepository.saveAndFlush(
                newStoreMap(store, 1, MapStatus.DRAFT));
        MapNode fromNode = persistNode(storeMap, MapNodeType.PATH, "From");
        MapNode toNode = persistNode(storeMap, MapNodeType.PATH, "To");
        MapEdge edge = newEntity(MapEdge.class);
        edge.setStoreMap(storeMap);
        edge.setFromNodeId(fromNode.getId());
        edge.setToNodeId(toNode.getId());
        edge.setDistanceMeters(BigDecimal.ONE);
        edge.setBidirectional(true);
        edge = mapEdgeRepository.saveAndFlush(edge);

        entityManager.clear();
        storeMapRepository.deleteById(storeMap.getId());
        storeMapRepository.flush();
        entityManager.clear();

        assertFalse(mapEdgeRepository.existsById(edge.getId()));
        assertFalse(mapNodeRepository.existsById(fromNode.getId()));
        assertFalse(mapNodeRepository.existsById(toNode.getId()));
    }

    @Test
    void cascadesStoreDeleteToMapsAndStoreProductsButKeepsProduct() {
        Store store = storeRepository.saveAndFlush(newStore("STORE-13"));
        Category category = categoryRepository.saveAndFlush(newCategory("CATEGORY-13"));
        Product product = productRepository.saveAndFlush(
                newProduct(category, "SKU-13", "7890000000013"));
        StoreProduct storeProduct = storeProductRepository.saveAndFlush(
                newStoreProduct(store, product));
        StoreMap storeMap = storeMapRepository.saveAndFlush(
                newStoreMap(store, 1, MapStatus.DRAFT));

        entityManager.clear();
        storeRepository.deleteById(store.getId());
        storeRepository.flush();
        entityManager.clear();

        assertFalse(storeMapRepository.existsById(storeMap.getId()));
        assertFalse(storeProductRepository.existsById(storeProduct.getId()));
        assertTrue(productRepository.existsById(product.getId()));
    }

    @Test
    void restrictsProductDeleteWhileStoreProductExists() {
        Store store = storeRepository.saveAndFlush(newStore("STORE-11"));
        Category category = categoryRepository.saveAndFlush(newCategory("CATEGORY-11"));
        Product product = productRepository.saveAndFlush(
                newProduct(category, "SKU-11", "7890000000011"));
        storeProductRepository.saveAndFlush(newStoreProduct(store, product));

        entityManager.clear();

        assertThrows(DataIntegrityViolationException.class, () -> {
            productRepository.deleteById(product.getId());
            productRepository.flush();
        });
    }

    private Store newStore(String code) {
        Store store = newEntity(Store.class);
        store.setName("Store " + code);
        store.setCode(code);
        store.setAddress("Address");
        store.setCity("City");
        store.setState("SP");
        return store;
    }

    private Category newCategory(String code) {
        Category category = newEntity(Category.class);
        category.setName("Category " + code);
        category.setCode(code);
        return category;
    }

    private Product newProduct(Category category, String sku, String ean) {
        Product product = newEntity(Product.class);
        product.setCategory(category);
        product.setSku(sku);
        product.setEan(ean);
        product.setName("Product " + sku);
        return product;
    }

    private StoreProduct newStoreProduct(Store store, Product product) {
        StoreProduct storeProduct = newEntity(StoreProduct.class);
        storeProduct.setStore(store);
        storeProduct.setProduct(product);
        return storeProduct;
    }

    private StoreMap newStoreMap(Store store, int version, MapStatus status) {
        StoreMap storeMap = newEntity(StoreMap.class);
        storeMap.setStore(store);
        storeMap.setVersion(version);
        storeMap.setName("Map " + version);
        storeMap.setWidth(new BigDecimal("100.0000"));
        storeMap.setHeight(new BigDecimal("80.0000"));
        storeMap.setScaleMetersPerUnit(new BigDecimal("1.000000"));
        storeMap.setStatus(status);
        return storeMap;
    }

    private Sector persistSector(StoreMap storeMap, String code) {
        Sector sector = newEntity(Sector.class);
        sector.setStoreMap(storeMap);
        sector.setName("Sector " + code);
        sector.setCode(code);
        setDimensions(sector, new BigDecimal("1.0000"));
        entityManager.persist(sector);
        entityManager.flush();
        return sector;
    }

    private Aisle persistAisle(StoreMap storeMap, Sector sector, String code) {
        Aisle aisle = newEntity(Aisle.class);
        aisle.setStoreMap(storeMap);
        aisle.setSectorId(sector.getId());
        aisle.setName("Aisle " + code);
        aisle.setCode(code);
        setDimensions(aisle, new BigDecimal("2.0000"));
        entityManager.persist(aisle);
        entityManager.flush();
        return aisle;
    }

    private ShelfBlock persistShelfBlock(StoreMap storeMap, Sector sector, Aisle aisle, String code) {
        ShelfBlock shelfBlock = newEntity(ShelfBlock.class);
        shelfBlock.setStoreMap(storeMap);
        shelfBlock.setSectorId(sector.getId());
        shelfBlock.setAisleId(aisle.getId());
        shelfBlock.setName("Shelf " + code);
        shelfBlock.setCode(code);
        setDimensions(shelfBlock, new BigDecimal("3.0000"));
        entityManager.persist(shelfBlock);
        entityManager.flush();
        return shelfBlock;
    }

    private void setDimensions(Sector sector, BigDecimal size) {
        sector.setX(BigDecimal.ZERO);
        sector.setY(BigDecimal.ZERO);
        sector.setWidth(size);
        sector.setHeight(size);
        sector.setRotation(BigDecimal.ZERO);
    }

    private void setDimensions(Aisle aisle, BigDecimal size) {
        aisle.setX(BigDecimal.ZERO);
        aisle.setY(BigDecimal.ZERO);
        aisle.setWidth(size);
        aisle.setHeight(size);
        aisle.setRotation(BigDecimal.ZERO);
    }

    private void setDimensions(ShelfBlock shelfBlock, BigDecimal size) {
        shelfBlock.setX(BigDecimal.ZERO);
        shelfBlock.setY(BigDecimal.ZERO);
        shelfBlock.setWidth(size);
        shelfBlock.setHeight(size);
        shelfBlock.setRotation(BigDecimal.ZERO);
    }

    private MapNode persistNode(StoreMap storeMap, MapNodeType type, String label) {
        MapNode node = newEntity(MapNode.class);
        node.setStoreMap(storeMap);
        node.setType(type);
        node.setX(BigDecimal.TEN);
        node.setY(BigDecimal.TEN);
        node.setLabel(label);
        return mapNodeRepository.saveAndFlush(node);
    }

    private <T> T newEntity(Class<T> type) {
        try {
            Constructor<T> constructor = type.getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException exception) {
            throw new AssertionError("Could not instantiate " + type.getName(), exception);
        }
    }
}
