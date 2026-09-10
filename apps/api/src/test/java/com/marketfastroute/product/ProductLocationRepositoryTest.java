package com.marketfastroute.product;

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

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class ProductLocationRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ProductLocationRepository productLocationRepository;

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
    void listsOnlyActiveLocationsForTheRequestedStoreAndProduct() {
        UUID storeId = UUID.randomUUID();
        UUID otherStoreId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID otherProductId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        UUID nodeId = UUID.randomUUID();
        UUID otherMapId = UUID.randomUUID();
        UUID otherNodeId = UUID.randomUUID();
        UUID storeProductId = UUID.randomUUID();
        UUID otherStoreProductId = UUID.randomUUID();

        insertStore(storeId);
        insertStore(otherStoreId);
        insertCategory(categoryId);
        insertProduct(productId, categoryId, "SKU-LOCATION", true);
        insertProduct(otherProductId, categoryId, "SKU-OTHER", true);
        insertStoreProduct(storeProductId, storeId, productId, true);
        insertStoreProduct(otherStoreProductId, storeId, otherProductId, true);
        insertStoreMap(mapId, storeId);
        insertMapNode(nodeId, mapId);
        insertStoreMap(otherMapId, otherStoreId);
        insertMapNode(otherNodeId, otherMapId);

        UUID primaryId = UUID.randomUUID();
        UUID secondaryId = UUID.randomUUID();
        insertLocation(primaryId, storeId, storeProductId, mapId, nodeId, true, true);
        insertLocation(secondaryId, storeId, storeProductId, mapId, nodeId, false, true);
        insertLocation(UUID.randomUUID(), storeId, storeProductId, mapId, nodeId, false, false);
        insertLocation(UUID.randomUUID(), storeId, otherStoreProductId, mapId, nodeId, false, true);

        List<ProductLocation> result = productLocationRepository
                .findActiveByStoreIdAndMapIdAndProductId(storeId, mapId, productId);

        assertEquals(List.of(primaryId, secondaryId), result.stream().map(ProductLocation::getId).toList());
        assertEquals(productId, result.getFirst().getStoreProduct().getProduct().getId());
        assertEquals(storeId, result.getFirst().getStoreMap().getStore().getId());
        assertEquals(mapId, result.getFirst().getNavigationNode().getStoreMap().getId());
    }

    @Test
    void findsOnlyTheActivePrimaryLocation() {
        UUID storeId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        UUID nodeId = UUID.randomUUID();
        UUID storeProductId = UUID.randomUUID();
        insertStore(storeId);
        insertCategory(categoryId);
        insertProduct(productId, categoryId, "SKU-PRIMARY", true);
        insertStoreProduct(storeProductId, storeId, productId, true);
        insertStoreMap(mapId, storeId);
        insertMapNode(nodeId, mapId);
        UUID primaryId = UUID.randomUUID();
        UUID secondaryId = UUID.randomUUID();
        insertLocation(primaryId, storeId, storeProductId, mapId, nodeId, true, true);
        insertLocation(secondaryId, storeId, storeProductId, mapId, nodeId, false, true);

        List<ProductLocation> result = productLocationRepository
                .findActivePrimaryByStoreIdAndMapId(storeId, mapId, productId);

        assertEquals(List.of(primaryId), result.stream().map(ProductLocation::getId).toList());
        assertTrue(result.getFirst().isPrimaryLocation());
    }

    @Test
    void scopesLocationsToTheRequestedMapVersion() {
        UUID storeId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID storeProductId = UUID.randomUUID();
        UUID activeMapId = UUID.randomUUID();
        UUID archivedMapId = UUID.randomUUID();
        UUID activeNodeId = UUID.randomUUID();
        UUID archivedNodeId = UUID.randomUUID();
        UUID activeLocationId = UUID.randomUUID();
        UUID archivedLocationId = UUID.randomUUID();

        insertStore(storeId);
        insertCategory(categoryId);
        insertProduct(productId, categoryId, "SKU-MAP-VERSION", true);
        insertStoreProduct(storeProductId, storeId, productId, true);
        insertStoreMap(activeMapId, storeId, 1, "ACTIVE");
        insertStoreMap(archivedMapId, storeId, 2, "ARCHIVED");
        insertMapNode(activeNodeId, activeMapId);
        insertMapNode(archivedNodeId, archivedMapId);
        insertLocation(activeLocationId, storeId, storeProductId, activeMapId, activeNodeId, true, true);
        insertLocation(archivedLocationId, storeId, storeProductId, archivedMapId, archivedNodeId, true, true);

        List<ProductLocation> result = productLocationRepository
                .findActiveByStoreIdAndMapIdAndProductId(storeId, activeMapId, productId);
        List<ProductLocation> primaryResult = productLocationRepository
                .findActivePrimaryByStoreIdAndMapId(storeId, activeMapId, productId);

        assertEquals(List.of(activeLocationId), result.stream().map(ProductLocation::getId).toList());
        assertEquals(List.of(activeLocationId), primaryResult.stream().map(ProductLocation::getId).toList());
    }

    @Test
    void findsARequestedLocationOnlyWhenItMatchesStoreProductAndActiveFlags() {
        UUID storeId = UUID.randomUUID();
        UUID otherStoreId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID otherProductId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        UUID nodeId = UUID.randomUUID();
        UUID storeProductId = UUID.randomUUID();
        UUID otherStoreProductId = UUID.randomUUID();
        insertStore(storeId);
        insertStore(otherStoreId);
        insertCategory(categoryId);
        insertProduct(productId, categoryId, "SKU-SPECIFIC", true);
        insertProduct(otherProductId, categoryId, "SKU-SPECIFIC-OTHER", true);
        insertStoreProduct(storeProductId, storeId, productId, true);
        insertStoreProduct(otherStoreProductId, otherStoreId, otherProductId, true);
        insertStoreMap(mapId, storeId);
        insertMapNode(nodeId, mapId);
        UUID locationId = UUID.randomUUID();
        insertLocation(locationId, storeId, storeProductId, mapId, nodeId, false, true);

        assertTrue(productLocationRepository
                .findActiveByIdAndStoreIdAndMapIdAndProductId(locationId, storeId, mapId, productId).isPresent());
        assertTrue(productLocationRepository
                .findActiveByIdAndStoreIdAndMapIdAndProductId(
                        locationId, otherStoreId, mapId, otherProductId).isEmpty());
        assertTrue(productLocationRepository
                .findActiveByIdAndStoreIdAndMapIdAndProductId(
                        locationId, storeId, mapId, otherProductId).isEmpty());
    }

    @Test
    void excludesLocationsForInactiveStoreProductsAndProducts() {
        UUID storeId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID inactiveAssociationProductId = UUID.randomUUID();
        UUID inactiveProductId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        UUID nodeId = UUID.randomUUID();
        UUID inactiveAssociationId = UUID.randomUUID();
        UUID inactiveProductAssociationId = UUID.randomUUID();
        insertStore(storeId);
        insertCategory(categoryId);
        insertProduct(inactiveAssociationProductId, categoryId, "SKU-INACTIVE-ASSOCIATION", true);
        insertProduct(inactiveProductId, categoryId, "SKU-INACTIVE-PRODUCT", false);
        insertStoreProduct(inactiveAssociationId, storeId, inactiveAssociationProductId, false);
        insertStoreProduct(inactiveProductAssociationId, storeId, inactiveProductId, true);
        insertStoreMap(mapId, storeId);
        insertMapNode(nodeId, mapId);
        insertLocation(UUID.randomUUID(), storeId, inactiveAssociationId, mapId, nodeId, false, true);
        insertLocation(UUID.randomUUID(), storeId, inactiveProductAssociationId, mapId, nodeId, false, true);

        assertFalse(productLocationRepository.findActiveByStoreIdAndMapIdAndProductId(
                storeId, mapId, inactiveAssociationProductId).iterator().hasNext());
        assertFalse(productLocationRepository.findActiveByStoreIdAndMapIdAndProductId(
                storeId, mapId, inactiveProductId).iterator().hasNext());
    }

    private void insertStore(UUID storeId) {
        jdbcTemplate.update(
                "INSERT INTO store (id, name, code, address, city, state) VALUES (?, ?, ?, ?, ?, ?)",
                storeId, "Store", storeId.toString(), "Address", "City", "SP");
    }

    private void insertCategory(UUID categoryId) {
        jdbcTemplate.update(
                "INSERT INTO category (id, name, code) VALUES (?, ?, ?)",
                categoryId, "Category", categoryId.toString());
    }

    private void insertProduct(UUID productId, UUID categoryId, String sku, boolean active) {
        jdbcTemplate.update(
                "INSERT INTO product (id, category_id, sku, name, active) VALUES (?, ?, ?, ?, ?)",
                productId, categoryId, sku, "Product " + sku, active);
    }

    private void insertStoreProduct(UUID storeProductId, UUID storeId, UUID productId, boolean active) {
        jdbcTemplate.update(
                "INSERT INTO store_product (id, store_id, product_id, active) VALUES (?, ?, ?, ?)",
                storeProductId, storeId, productId, active);
    }

    private void insertStoreMap(UUID mapId, UUID storeId) {
        insertStoreMap(mapId, storeId, 1, "ACTIVE");
    }

    private void insertStoreMap(UUID mapId, UUID storeId, int version, String status) {
        jdbcTemplate.update("""
                INSERT INTO store_map
                    (id, store_id, version, name, width, height, scale_meters_per_unit, status)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """, mapId, storeId, version, "Map", 100, 80, 1, status);
    }

    private void insertMapNode(UUID nodeId, UUID mapId) {
        jdbcTemplate.update("""
                INSERT INTO map_node (id, map_id, type, x, y, label)
                VALUES (?, ?, 'PRODUCT_ACCESS', 10, 20, 'Product')
                """, nodeId, mapId);
    }

    private void insertLocation(
            UUID locationId,
            UUID storeId,
            UUID storeProductId,
            UUID mapId,
            UUID nodeId,
            boolean primary,
            boolean active
    ) {
        jdbcTemplate.update("""
                INSERT INTO product_location
                    (id, store_id, store_product_id, map_id, navigation_node_id,
                     primary_location, active)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, locationId, storeId, storeProductId, mapId, nodeId, primary, active);
    }
}
