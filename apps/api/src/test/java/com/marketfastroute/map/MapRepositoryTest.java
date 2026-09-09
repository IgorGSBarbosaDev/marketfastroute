package com.marketfastroute.map;

import com.marketfastroute.store.StoreMap;
import com.marketfastroute.store.StoreMapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Transactional
class MapRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private StoreMapRepository storeMapRepository;

    @Autowired
    private SectorRepository sectorRepository;

    @Autowired
    private AisleRepository aisleRepository;

    @Autowired
    private ShelfBlockRepository shelfBlockRepository;

    @Autowired
    private PointOfInterestRepository pointOfInterestRepository;

    @Autowired
    private MapNodeRepository mapNodeRepository;

    @Autowired
    private MapEdgeRepository mapEdgeRepository;

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
    void loadsOnlyActiveElementsFromTheRequestedMap() {
        UUID storeId = UUID.randomUUID();
        UUID otherStoreId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        UUID otherMapId = UUID.randomUUID();
        UUID fromNodeId = UUID.randomUUID();
        UUID toNodeId = UUID.randomUUID();

        insertStore(storeId);
        insertStore(otherStoreId);
        insertMap(mapId, storeId, 1, MapStatus.ACTIVE);
        insertMap(UUID.randomUUID(), storeId, 2, MapStatus.DRAFT);
        insertMap(otherMapId, otherStoreId, 1, MapStatus.ACTIVE);

        UUID activeSectorId = UUID.randomUUID();
        insertSector(activeSectorId, mapId, true);
        insertSector(UUID.randomUUID(), mapId, false);
        insertSector(UUID.randomUUID(), otherMapId, true);

        UUID activeAisleId = UUID.randomUUID();
        insertAisle(activeAisleId, mapId, activeSectorId, true);
        insertAisle(UUID.randomUUID(), mapId, activeSectorId, false);
        insertAisle(UUID.randomUUID(), otherMapId, null, true);

        UUID activeShelfBlockId = UUID.randomUUID();
        insertShelfBlock(activeShelfBlockId, mapId, activeSectorId, activeAisleId, true);
        insertShelfBlock(UUID.randomUUID(), mapId, activeSectorId, activeAisleId, false);
        insertShelfBlock(UUID.randomUUID(), otherMapId, null, null, true);

        insertMapNode(fromNodeId, mapId, MapNodeType.ENTRANCE, true);
        insertMapNode(toNodeId, mapId, MapNodeType.CHECKOUT, true);
        UUID inactiveNodeId = UUID.randomUUID();
        insertMapNode(inactiveNodeId, mapId, MapNodeType.PATH, false);
        insertMapNode(UUID.randomUUID(), otherMapId, MapNodeType.PATH, true);

        UUID activePointId = UUID.randomUUID();
        insertPointOfInterest(activePointId, mapId, toNodeId, true);
        insertPointOfInterest(UUID.randomUUID(), mapId, toNodeId, false);
        insertPointOfInterest(UUID.randomUUID(), otherMapId, null, true);

        UUID activeEdgeId = UUID.randomUUID();
        insertMapEdge(activeEdgeId, mapId, fromNodeId, toNodeId, true);
        insertMapEdge(UUID.randomUUID(), mapId, fromNodeId, inactiveNodeId, false);

        assertEquals(mapId, storeMapRepository.findActiveByStoreId(storeId)
                .map(StoreMap::getId).orElseThrow());
        assertEquals(List.of(activeSectorId), sectorRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)
                .stream().map(Sector::getId).toList());
        assertEquals(List.of(activeAisleId), aisleRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)
                .stream().map(Aisle::getId).toList());
        assertEquals(List.of(activeShelfBlockId),
                shelfBlockRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)
                        .stream().map(ShelfBlock::getId).toList());
        assertEquals(List.of(activePointId),
                pointOfInterestRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)
                        .stream().map(PointOfInterest::getId).toList());
        assertTrue(mapNodeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId).stream()
                .map(MapNode::getId)
                .allMatch(List.of(fromNodeId, toNodeId)::contains));
        assertEquals(List.of(activeEdgeId), mapEdgeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)
                .stream().map(MapEdge::getId).toList());
    }

    private void insertStore(UUID storeId) {
        jdbcTemplate.update(
                "INSERT INTO store (id, name, code, address, city, state) VALUES (?, ?, ?, ?, ?, ?)",
                storeId, "Store", storeId.toString(), "Address", "City", "SP");
    }

    private void insertMap(UUID mapId, UUID storeId, int version, MapStatus status) {
        jdbcTemplate.update("""
                INSERT INTO store_map
                    (id, store_id, version, name, width, height, scale_meters_per_unit, status)
                VALUES (?, ?, ?, ?, 100, 80, 1, ?)
                """, mapId, storeId, version, "Map " + version, status.name());
    }

    private void insertSector(UUID id, UUID mapId, boolean active) {
        jdbcTemplate.update("""
                INSERT INTO sector (id, map_id, name, code, x, y, width, height, rotation, active)
                VALUES (?, ?, 'Sector', ?, 0, 0, 10, 10, 0, ?)
                """, id, mapId, id.toString(), active);
    }

    private void insertAisle(UUID id, UUID mapId, UUID sectorId, boolean active) {
        jdbcTemplate.update("""
                INSERT INTO aisle (id, map_id, sector_id, code, name, x, y, width, height, rotation, active)
                VALUES (?, ?, ?, ?, 'Aisle', 0, 0, 10, 10, 0, ?)
                """, id, mapId, sectorId, id.toString(), active);
    }

    private void insertShelfBlock(UUID id, UUID mapId, UUID sectorId, UUID aisleId, boolean active) {
        jdbcTemplate.update("""
                INSERT INTO shelf_block
                    (id, map_id, sector_id, aisle_id, code, name, x, y, width, height, rotation, active)
                VALUES (?, ?, ?, ?, ?, 'Shelf', 0, 0, 10, 10, 0, ?)
                """, id, mapId, sectorId, aisleId, id.toString(), active);
    }

    private void insertMapNode(UUID id, UUID mapId, MapNodeType type, boolean active) {
        jdbcTemplate.update("""
                INSERT INTO map_node (id, map_id, type, x, y, label, active)
                VALUES (?, ?, ?, 0, 0, 'Node', ?)
                """, id, mapId, type.name(), active);
    }

    private void insertPointOfInterest(UUID id, UUID mapId, UUID navigationNodeId, boolean active) {
        jdbcTemplate.update("""
                INSERT INTO point_of_interest (id, map_id, navigation_node_id, type, name, x, y, active)
                VALUES (?, ?, ?, 'CHECKOUT', 'Checkout', 0, 0, ?)
                """, id, mapId, navigationNodeId, active);
    }

    private void insertMapEdge(UUID id, UUID mapId, UUID fromNodeId, UUID toNodeId, boolean active) {
        jdbcTemplate.update("""
                INSERT INTO map_edge
                    (id, map_id, from_node_id, to_node_id, distance_meters, bidirectional, active)
                VALUES (?, ?, ?, ?, 10, TRUE, ?)
                """, id, mapId, fromNodeId, toNodeId, active);
    }
}
