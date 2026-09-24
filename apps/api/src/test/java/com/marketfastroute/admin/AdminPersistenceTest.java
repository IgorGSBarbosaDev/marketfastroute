package com.marketfastroute.admin;

import com.marketfastroute.admin.dto.CreateCategoryRequest;
import com.marketfastroute.admin.dto.CreateAisleRequest;
import com.marketfastroute.admin.dto.CreateMapEdgeRequest;
import com.marketfastroute.admin.dto.CreateMapNodeRequest;
import com.marketfastroute.admin.dto.CreateProductLocationRequest;
import com.marketfastroute.admin.dto.CreateProductRequest;
import com.marketfastroute.admin.dto.CreateSectorRequest;
import com.marketfastroute.admin.dto.CreateStoreMapRequest;
import com.marketfastroute.admin.dto.CreateStoreProductRequest;
import com.marketfastroute.admin.dto.CreateStoreRequest;
import com.marketfastroute.admin.dto.UpdateStoreMapRequest;
import com.marketfastroute.map.MapGraphAdminService;
import com.marketfastroute.map.MapStructureAdminService;
import com.marketfastroute.map.MapNodeType;
import com.marketfastroute.map.MapStatus;
import com.marketfastroute.product.CatalogAdminService;
import com.marketfastroute.product.ProductLocationAdminService;
import com.marketfastroute.store.StoreAdminService;
import com.marketfastroute.store.StoreMapAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class AdminPersistenceTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StoreAdminService storeAdminService;

    @Autowired
    private CatalogAdminService catalogAdminService;

    @Autowired
    private StoreMapAdminService storeMapAdminService;

    @Autowired
    private MapGraphAdminService mapGraphAdminService;

    @Autowired
    private MapStructureAdminService mapStructureAdminService;

    @Autowired
    private ProductLocationAdminService productLocationAdminService;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("""
                TRUNCATE TABLE
                    product_location,
                    map_edge,
                    point_of_interest,
                    map_node,
                    shelf_block,
                    aisle,
                    sector,
                    store_map,
                    store_product,
                    product,
                    category,
                    store
                CASCADE
                """);
    }

    @Test
    void persistsTheAdministrativeCatalogMapAndLocationFlow() {
        UUID storeId = storeAdminService.create(
                new CreateStoreRequest("Store", "STORE-ADMIN", "Address", "City", "SP")).id();
        UUID categoryId = catalogAdminService.createCategory(
                new CreateCategoryRequest("Dairy", "DAIRY-ADMIN", null)).id();
        UUID productId = catalogAdminService.createProduct(
                new CreateProductRequest(categoryId, "SKU-ADMIN", "7890000000999", "Milk", "Brand", null)).id();
        UUID storeProductId = catalogAdminService.createStoreProduct(
                storeId, new CreateStoreProductRequest(productId, true)).id();
        UUID mapId = storeMapAdminService.create(storeId, new CreateStoreMapRequest(
                1, "Draft", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, MapStatus.DRAFT)).id();
        UUID nodeId = mapGraphAdminService.createNode(mapId, new CreateMapNodeRequest(
                MapNodeType.PRODUCT_ACCESS, BigDecimal.ONE, BigDecimal.ONE, "Milk", true)).id();

        var location = productLocationAdminService.create(storeId, new CreateProductLocationRequest(
                storeProductId, mapId, null, null, null, null, null, null,
                BigDecimal.ONE, BigDecimal.ONE, nodeId, true, true));

        assertEquals(storeId, location.storeId());
        assertEquals(storeProductId, location.storeProductId());
        assertEquals(mapId, location.mapId());
        assertEquals(nodeId, location.navigationNodeId());
    }

    @Test
    void activatesOneMapAndRejectsASecondActiveMapForTheSameStore() {
        UUID storeId = storeAdminService.create(
                new CreateStoreRequest("Store", "STORE-MAPS", "Address", "City", "SP")).id();
        UUID storeProductId = createStoreProduct(storeId);
        UUID firstMapId = createPublishableMap(storeId, storeProductId, 1, "Map 1");
        UUID secondMapId = createPublishableMap(storeId, storeProductId, 2, "Map 2");

        var firstActive = storeMapAdminService.update(storeId, firstMapId, new UpdateStoreMapRequest(
                1, "Map 1", new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ONE, MapStatus.ACTIVE));
        assertEquals(MapStatus.ACTIVE, firstActive.status());
        assertThrows(AdminValidationException.class, () -> mapGraphAdminService.createNode(
                firstMapId, new CreateMapNodeRequest(
                        MapNodeType.PATH, new BigDecimal("50"), new BigDecimal("70"), "Late edit", true)));

        assertThrows(AdminConflictException.class, () -> storeMapAdminService.update(
                storeId, secondMapId, new UpdateStoreMapRequest(
                        2, "Map 2", new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ONE, MapStatus.ACTIVE)));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void serializesConcurrentActivationsForSeparateDraftMaps() throws Exception {
        try {
            UUID storeId = storeAdminService.create(
                    new CreateStoreRequest("Store", "STORE-CONCURRENT", "Address", "City", "SP")).id();
            UUID storeProductId = createStoreProduct(storeId);
            UUID firstMapId = createPublishableMap(storeId, storeProductId, 1, "Map 1");
            UUID secondMapId = createPublishableMap(storeId, storeProductId, 2, "Map 2");
            CountDownLatch start = new CountDownLatch(1);

            try (var executor = Executors.newFixedThreadPool(2)) {
                var first = executor.submit(() -> activateAfter(start, storeId, firstMapId, 1, "Map 1"));
                var second = executor.submit(() -> activateAfter(start, storeId, secondMapId, 2, "Map 2"));
                start.countDown();

                var outcomes = java.util.List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
                assertEquals(1, outcomes.stream().filter("ACTIVE"::equals).count());
                assertEquals(1, outcomes.stream().filter("CONFLICT"::equals).count());
                assertEquals(1L, jdbcTemplate.queryForObject(
                        "select count(*) from store_map where store_id = ? and status = 'ACTIVE'",
                        Long.class,
                        storeId));
            }
        } finally {
            cleanDatabase();
        }
    }

    private String activateAfter(
            CountDownLatch start,
            UUID storeId,
            UUID mapId,
            int version,
            String name
    ) throws InterruptedException {
        start.await();
        try {
            return storeMapAdminService.update(storeId, mapId, new UpdateStoreMapRequest(
                    version, name, new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ONE,
                    MapStatus.ACTIVE)).status().name();
        } catch (AdminConflictException exception) {
            return "CONFLICT";
        }
    }

    private UUID createStoreProduct(UUID storeId) {
        UUID categoryId = catalogAdminService.createCategory(
                new CreateCategoryRequest("Category", "CAT-MAPS", null)).id();
        UUID productId = catalogAdminService.createProduct(
                new CreateProductRequest(categoryId, "SKU-MAPS", "7890000000888", "Product", "Brand", null)).id();
        return catalogAdminService.createStoreProduct(
                storeId, new CreateStoreProductRequest(productId, true)).id();
    }

    private UUID createPublishableMap(UUID storeId, UUID storeProductId, int version, String name) {
        UUID mapId = storeMapAdminService.create(storeId, new CreateStoreMapRequest(
                version, name, new BigDecimal("100"), new BigDecimal("100"), BigDecimal.ONE, MapStatus.DRAFT)).id();
        UUID sectorId = mapStructureAdminService.createSector(mapId, new CreateSectorRequest(
                "Sector", "S-" + version, new BigDecimal("10"), new BigDecimal("10"),
                new BigDecimal("80"), new BigDecimal("80"), BigDecimal.ZERO, true)).id();
        UUID aisleId = mapStructureAdminService.createAisle(mapId, new CreateAisleRequest(
                sectorId, "A-" + version, "Aisle", new BigDecimal("20"), new BigDecimal("40"),
                new BigDecimal("60"), new BigDecimal("10"), BigDecimal.ZERO, true)).id();
        UUID entranceId = mapGraphAdminService.createNode(mapId, new CreateMapNodeRequest(
                MapNodeType.ENTRANCE, new BigDecimal("10"), new BigDecimal("50"), "Entry", true)).id();
        UUID productNodeId = mapGraphAdminService.createNode(mapId, new CreateMapNodeRequest(
                MapNodeType.PRODUCT_ACCESS, new BigDecimal("50"), new BigDecimal("50"), "Products", true)).id();
        UUID checkoutId = mapGraphAdminService.createNode(mapId, new CreateMapNodeRequest(
                MapNodeType.CHECKOUT, new BigDecimal("90"), new BigDecimal("50"), "Checkout", true)).id();
        mapGraphAdminService.createEdge(mapId,
                new CreateMapEdgeRequest(entranceId, productNodeId, new BigDecimal("40"), true, true));
        mapGraphAdminService.createEdge(mapId,
                new CreateMapEdgeRequest(productNodeId, checkoutId, new BigDecimal("40"), true, true));
        productLocationAdminService.create(storeId, new CreateProductLocationRequest(
                storeProductId, mapId, sectorId, aisleId, null, null, "Module 1", 1,
                new BigDecimal("50"), new BigDecimal("50"), productNodeId, true, true));
        return mapId;
    }
}
