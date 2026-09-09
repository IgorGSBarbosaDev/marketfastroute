package com.marketfastroute;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ApiApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

	@Autowired
	JdbcTemplate jdbcTemplate;

	private int nextMapVersion;

	@BeforeEach
	void cleanDatabase() {
		nextMapVersion = 1;
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
	void contextLoads() {
		assertTrue(postgres.isRunning());
		assertEquals(4, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM flyway_schema_history WHERE success = TRUE", Integer.class));
		assertEquals(12, jdbcTemplate.queryForObject("""
				SELECT COUNT(*)
				FROM pg_tables
				WHERE schemaname = 'public'
				  AND tablename IN (
					  'store', 'category', 'product', 'store_product', 'store_map',
					  'sector', 'aisle', 'shelf_block', 'map_node',
					  'point_of_interest', 'map_edge', 'product_location'
				  )
				""", Integer.class));
		assertEquals(16, jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM pg_indexes WHERE schemaname = 'public' AND indexname LIKE 'ix_%'",
				Integer.class));
	}

	@Test
	void allowsAtMostOneActiveMapPerStore() {
		UUID storeId = UUID.randomUUID();
		insertStore(storeId);
		insertMap(storeId, "ACTIVE");

		assertThrows(DataIntegrityViolationException.class, () -> insertMap(storeId, "ACTIVE"));
	}

	@Test
	void allowsAtMostOnePrimaryLocationPerProductAndMap() {
		UUID storeId = UUID.randomUUID();
		UUID categoryId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		UUID storeProductId = UUID.randomUUID();
		UUID mapId = UUID.randomUUID();
		UUID nodeId = UUID.randomUUID();

		insertStore(storeId);
		insertCategory(categoryId);
		insertProduct(productId, categoryId);
		insertStoreProduct(storeProductId, storeId, productId);
		insertMap(mapId, storeId, 1, "DRAFT");
		insertMapNode(nodeId, mapId);
		insertProductLocation(storeId, storeProductId, mapId, nodeId, true);

		assertThrows(DataIntegrityViolationException.class,
				() -> insertProductLocation(storeId, storeProductId, mapId, nodeId, true));
	}

	@Test
	void rejectsProductLocationFromAnotherStore() {
		UUID firstStoreId = UUID.randomUUID();
		UUID secondStoreId = UUID.randomUUID();
		UUID categoryId = UUID.randomUUID();
		UUID productId = UUID.randomUUID();
		UUID storeProductId = UUID.randomUUID();
		UUID secondMapId = UUID.randomUUID();
		UUID secondNodeId = UUID.randomUUID();

		insertStore(firstStoreId);
		insertStore(secondStoreId);
		insertCategory(categoryId);
		insertProduct(productId, categoryId);
		insertStoreProduct(storeProductId, firstStoreId, productId);
		insertMap(UUID.randomUUID(), firstStoreId, 1, "DRAFT");
		insertMap(secondMapId, secondStoreId, 1, "DRAFT");
		insertMapNode(secondNodeId, secondMapId);

		assertThrows(DataIntegrityViolationException.class,
				() -> insertProductLocation(firstStoreId, storeProductId, secondMapId, secondNodeId, false));
	}

	private void insertStore(UUID storeId) {
		jdbcTemplate.update(
				"INSERT INTO store (id, name, code, address, city, state) VALUES (?, ?, ?, ?, ?, ?)",
				storeId, "Store", storeId.toString(), "Address", "City", "State");
	}

	private void insertCategory(UUID categoryId) {
		jdbcTemplate.update(
				"INSERT INTO category (id, name, code) VALUES (?, ?, ?)",
				categoryId, "Category", "CATEGORY-1");
	}

	private void insertProduct(UUID productId, UUID categoryId) {
		jdbcTemplate.update(
				"INSERT INTO product (id, category_id, sku, name) VALUES (?, ?, ?, ?)",
				productId, categoryId, "SKU-1", "Product");
	}

	private void insertStoreProduct(UUID storeProductId, UUID storeId, UUID productId) {
		jdbcTemplate.update(
				"INSERT INTO store_product (id, store_id, product_id) VALUES (?, ?, ?)",
				storeProductId, storeId, productId);
	}

	private void insertMap(UUID storeId, String status) {
		insertMap(UUID.randomUUID(), storeId, nextMapVersion++, status);
	}

	private void insertMap(UUID mapId, UUID storeId, int version, String status) {
		jdbcTemplate.update(
				"INSERT INTO store_map (id, store_id, version, name, width, height, scale_meters_per_unit, status) "
						+ "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
				mapId, storeId, version,
				"Map", 100, 100, 1, status);
	}

	private void insertMapNode(UUID nodeId, UUID mapId) {
		jdbcTemplate.update(
				"INSERT INTO map_node (id, map_id, type, x, y) VALUES (?, ?, ?, ?, ?)",
				nodeId, mapId, "PRODUCT_ACCESS", 10, 10);
	}

	private void insertProductLocation(
				UUID storeId,
				UUID storeProductId,
				UUID mapId,
				UUID nodeId,
				boolean primaryLocation) {
		jdbcTemplate.update(
				"INSERT INTO product_location "
						+ "(store_id, store_product_id, map_id, navigation_node_id, primary_location) "
						+ "VALUES (?, ?, ?, ?, ?)",
				storeId, storeProductId, mapId, nodeId, primaryLocation);
	}
}
