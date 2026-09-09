package com.marketfastroute.admin;

import com.marketfastroute.admin.dto.CreateCategoryRequest;
import com.marketfastroute.admin.dto.CreateMapNodeRequest;
import com.marketfastroute.admin.dto.CreateProductLocationRequest;
import com.marketfastroute.admin.dto.CreateProductRequest;
import com.marketfastroute.admin.dto.CreateStoreMapRequest;
import com.marketfastroute.admin.dto.CreateStoreProductRequest;
import com.marketfastroute.admin.dto.CreateStoreRequest;
import com.marketfastroute.admin.dto.UpdateStoreMapRequest;
import com.marketfastroute.map.MapGraphAdminService;
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
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

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
        UUID firstMapId = storeMapAdminService.create(storeId, new CreateStoreMapRequest(
                1, "Map 1", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, MapStatus.DRAFT)).id();
        UUID secondMapId = storeMapAdminService.create(storeId, new CreateStoreMapRequest(
                2, "Map 2", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, MapStatus.DRAFT)).id();

        var firstActive = storeMapAdminService.update(storeId, firstMapId, new UpdateStoreMapRequest(
                1, "Map 1", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, MapStatus.ACTIVE));
        assertEquals(MapStatus.ACTIVE, firstActive.status());

        assertThrows(AdminConflictException.class, () -> storeMapAdminService.update(
                storeId, secondMapId, new UpdateStoreMapRequest(
                        2, "Map 2", BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ONE, MapStatus.ACTIVE)));
    }
}
