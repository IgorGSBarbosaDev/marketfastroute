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
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class ProductRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StoreProductRepository storeProductRepository;

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
    void listsOnlyActiveProductsAssociatedWithTheRequestedStore() {
        UUID firstStoreId = UUID.randomUUID();
        UUID secondStoreId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID availableProductId = UUID.randomUUID();
        UUID inactiveProductId = UUID.randomUUID();
        UUID inactiveAssociationProductId = UUID.randomUUID();
        UUID inactiveStoreProductId = UUID.randomUUID();
        UUID otherStoreProductId = UUID.randomUUID();

        insertStore(firstStoreId);
        insertStore(secondStoreId);
        insertCategory(categoryId, "DAIRY", "Dairy");
        insertProduct(availableProductId, categoryId, "SKU-AVAILABLE", "7890000000101", "Available", true);
        insertProduct(inactiveProductId, categoryId, "SKU-INACTIVE", "7890000000102", "Inactive", false);
        insertProduct(inactiveAssociationProductId, categoryId, "SKU-STORE-INACTIVE", "7890000000103", "Store inactive", true);
        insertStoreProduct(UUID.randomUUID(), firstStoreId, availableProductId, true);
        insertStoreProduct(UUID.randomUUID(), firstStoreId, inactiveProductId, true);
        insertStoreProduct(inactiveStoreProductId, firstStoreId, inactiveAssociationProductId, false);
        insertStoreProduct(otherStoreProductId, secondStoreId, availableProductId, true);

        List<StoreProduct> result = storeProductRepository.findAvailableByStoreId(firstStoreId);

        assertEquals(List.of(availableProductId), result.stream()
                .map(StoreProduct::getProduct)
                .map(Product::getId)
                .toList());
        assertEquals("Dairy", result.getFirst().getProduct().getCategory().getName());
    }

    @Test
    void findsAProductOnlyWhenItIsActiveAndAvailableInTheRequestedStore() {
        UUID firstStoreId = UUID.randomUUID();
        UUID secondStoreId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID inactiveProductId = UUID.randomUUID();
        UUID inactiveAssociationProductId = UUID.randomUUID();
        UUID inactiveStoreProductId = UUID.randomUUID();

        insertStore(firstStoreId);
        insertStore(secondStoreId);
        insertCategory(categoryId, "DAIRY", "Dairy");
        insertProduct(productId, categoryId, "SKU-ACTIVE", "7890000000201", "Active", true);
        insertProduct(inactiveProductId, categoryId, "SKU-INACTIVE", "7890000000202", "Inactive", false);
        insertProduct(inactiveAssociationProductId, categoryId, "SKU-STORE-INACTIVE", "7890000000203", "Store inactive", true);
        insertStoreProduct(UUID.randomUUID(), firstStoreId, productId, true);
        insertStoreProduct(inactiveStoreProductId, firstStoreId, inactiveAssociationProductId, false);
        insertStoreProduct(UUID.randomUUID(), firstStoreId, inactiveProductId, true);

        assertTrue(storeProductRepository.findAvailableByStoreIdAndProductId(firstStoreId, productId).isPresent());
        assertTrue(storeProductRepository.findAvailableByStoreIdAndProductId(secondStoreId, productId).isEmpty());
        assertTrue(storeProductRepository.findAvailableByStoreIdAndProductId(firstStoreId, inactiveProductId).isEmpty());
        assertTrue(storeProductRepository.findAvailableByStoreIdAndProductId(firstStoreId, inactiveAssociationProductId).isEmpty());
    }

    @Test
    void searchesByNameSkuEanAndCategoryWithinTheRequestedStore() {
        UUID storeId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        UUID coffeeProductId = UUID.randomUUID();

        insertStore(storeId);
        insertCategory(categoryId, "BEVERAGES", "Beverages");
        insertProduct(productId, categoryId, "SKU-MILK", "7890000000301", "Integral Milk", true);
        insertProduct(coffeeProductId, categoryId, "SKU-COFFEE", "7890000000302", "Ground Coffee", true);
        insertStoreProduct(UUID.randomUUID(), storeId, productId, true);
        insertStoreProduct(UUID.randomUUID(), storeId, coffeeProductId, true);

        assertProductIds(List.of(productId), storeProductRepository.searchAvailableByStoreId(storeId, "milk"));
        assertProductIds(List.of(coffeeProductId), storeProductRepository.searchAvailableByStoreId(storeId, "sku-coffee"));
        assertProductIds(List.of(productId), storeProductRepository.searchAvailableByStoreId(storeId, "7890000000301"));
        assertProductIds(List.of(productId, coffeeProductId),
                storeProductRepository.searchAvailableByStoreId(storeId, "beverages"));
    }

    private void assertProductIds(List<UUID> expected, List<StoreProduct> result) {
        assertEquals(Set.copyOf(expected), result.stream()
                .map(StoreProduct::getProduct)
                .map(Product::getId)
                .collect(java.util.stream.Collectors.toSet()));
    }

    private void insertStore(UUID storeId) {
        jdbcTemplate.update(
                "INSERT INTO store (id, name, code, address, city, state) VALUES (?, ?, ?, ?, ?, ?)",
                storeId, "Store", storeId.toString(), "Address", "City", "SP");
    }

    private void insertCategory(UUID categoryId, String code, String name) {
        jdbcTemplate.update(
                "INSERT INTO category (id, name, code) VALUES (?, ?, ?)",
                categoryId, name, code + categoryId);
    }

    private void insertProduct(
            UUID productId,
            UUID categoryId,
            String sku,
            String ean,
            String name,
            boolean active
    ) {
        jdbcTemplate.update(
                "INSERT INTO product (id, category_id, sku, ean, name, brand, active) VALUES (?, ?, ?, ?, ?, ?, ?)",
                productId, categoryId, sku, ean, name, "Brand", active);
    }

    private void insertStoreProduct(UUID storeProductId, UUID storeId, UUID productId, boolean active) {
        jdbcTemplate.update(
                "INSERT INTO store_product (id, store_id, product_id, active) VALUES (?, ?, ?, ?)",
                storeProductId, storeId, productId, active);
    }
}
