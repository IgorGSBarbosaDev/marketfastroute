package com.marketfastroute.map;

import com.marketfastroute.store.Store;
import com.marketfastroute.store.StoreMap;
import com.marketfastroute.store.StoreMapRepository;
import com.marketfastroute.store.StoreNotFoundException;
import com.marketfastroute.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MapServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreMapRepository storeMapRepository;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private AisleRepository aisleRepository;

    @Mock
    private ShelfBlockRepository shelfBlockRepository;

    @Mock
    private PointOfInterestRepository pointOfInterestRepository;

    @Mock
    private MapNodeRepository mapNodeRepository;

    @Mock
    private MapEdgeRepository mapEdgeRepository;

    private MapService mapService;

    @BeforeEach
    void setUp() {
        mapService = new MapService(
                new ActiveStoreMapResolver(storeRepository, storeMapRepository),
                sectorRepository,
                aisleRepository,
                shelfBlockRepository,
                pointOfInterestRepository,
                mapNodeRepository,
                mapEdgeRepository,
                new MapMapper()
        );
    }

    @Test
    void loadsTheActiveMapWithItsVisualAndNavigationElements() {
        UUID storeId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        StoreMap storeMap = storeMap(storeId, mapId);
        Sector sector = sector(mapId, "Dairy");
        Aisle aisle = aisle(mapId, sector.getId());
        ShelfBlock shelfBlock = shelfBlock(mapId, sector.getId(), aisle.getId());
        PointOfInterest pointOfInterest = pointOfInterest(mapId, PointOfInterestType.CHECKOUT);
        MapNode node = node(mapId, MapNodeType.CHECKOUT);
        MapEdge edge = edge(mapId, UUID.randomUUID(), node.getId());

        givenActiveMap(storeId, storeMap);
        when(sectorRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of(sector));
        when(aisleRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of(aisle));
        when(shelfBlockRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId))
                .thenReturn(List.of(shelfBlock));
        when(pointOfInterestRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId))
                .thenReturn(List.of(pointOfInterest));
        when(mapNodeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of(node));
        when(mapEdgeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of(edge));

        StoreMapResponse result = mapService.findActiveByStore(storeId);

        assertEquals(mapId, result.id());
        assertEquals(storeId, result.storeId());
        assertEquals(List.of(sector.getId()), result.sectors().stream().map(SectorResponse::id).toList());
        assertEquals(List.of(aisle.getId()), result.aisles().stream().map(AisleResponse::id).toList());
        assertEquals(List.of(shelfBlock.getId()), result.shelfBlocks().stream().map(ShelfBlockResponse::id).toList());
        assertEquals(List.of(pointOfInterest.getId()),
                result.pointsOfInterest().stream().map(PointOfInterestResponse::id).toList());
        assertEquals(List.of(node.getId()), result.nodes().stream().map(MapNodeResponse::id).toList());
        assertEquals(List.of(edge.getId()), result.edges().stream().map(MapEdgeResponse::id).toList());
        verify(storeMapRepository).findActiveByStoreId(storeId);
    }

    @Test
    void rejectsAnInactiveOrUnknownStoreBeforeLoadingTheMap() {
        UUID storeId = UUID.randomUUID();
        when(storeRepository.existsByIdAndActiveTrue(storeId)).thenReturn(false);

        assertThrows(StoreNotFoundException.class, () -> mapService.findActiveByStore(storeId));

        verifyNoInteractions(storeMapRepository, sectorRepository, aisleRepository, shelfBlockRepository,
                pointOfInterestRepository, mapNodeRepository, mapEdgeRepository);
    }

    @Test
    void rejectsAStoreWithoutAnActiveMap() {
        UUID storeId = UUID.randomUUID();
        when(storeRepository.existsByIdAndActiveTrue(storeId)).thenReturn(true);
        when(storeMapRepository.findActiveByStoreId(storeId)).thenReturn(Optional.empty());

        assertThrows(StoreMapNotFoundException.class, () -> mapService.findActiveByStore(storeId));

        verifyNoInteractions(sectorRepository, aisleRepository, shelfBlockRepository,
                pointOfInterestRepository, mapNodeRepository, mapEdgeRepository);
    }

    @Test
    void excludesInactiveElementsReturnedByARepository() {
        UUID storeId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        StoreMap storeMap = storeMap(storeId, mapId);
        Sector inactiveSector = sector(mapId, "Inactive");
        when(inactiveSector.isActive()).thenReturn(false);

        givenActiveMap(storeId, storeMap);
        when(sectorRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of(inactiveSector));
        givenEmptyElementRepositories(mapId);

        StoreMapResponse result = mapService.findActiveByStore(storeId);

        assertTrue(result.sectors().isEmpty());
        assertTrue(result.aisles().isEmpty());
        assertTrue(result.nodes().isEmpty());
    }

    @Test
    void rejectsAnActiveElementFromAnotherMap() {
        UUID storeId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        UUID otherMapId = UUID.randomUUID();
        StoreMap storeMap = storeMap(storeId, mapId);
        Sector foreignSector = sector(otherMapId, "Foreign");

        givenActiveMap(storeId, storeMap);
        when(sectorRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of(foreignSector));

        assertThrows(MapConsistencyException.class, () -> mapService.findActiveByStore(storeId));

        verify(aisleRepository, never()).findByStoreMap_IdAndActiveTrueOrderById(mapId);
    }

    @Test
    void rejectsAReferenceToAnotherMapFromAPointOfInterestOrEdge() {
        UUID storeId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        UUID otherMapId = UUID.randomUUID();
        StoreMap storeMap = storeMap(storeId, mapId);
        PointOfInterest pointOfInterest = pointOfInterest(mapId, PointOfInterestType.ENTRANCE);
        MapNode foreignNode = node(otherMapId, MapNodeType.ENTRANCE);
        when(pointOfInterest.getNavigationNode()).thenReturn(foreignNode);

        givenActiveMap(storeId, storeMap);
        givenEmptyElementRepositories(mapId);
        when(pointOfInterestRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId))
                .thenReturn(List.of(pointOfInterest));

        assertThrows(MapConsistencyException.class, () -> mapService.findActiveByStore(storeId));
    }

    private void givenActiveMap(UUID storeId, StoreMap storeMap) {
        when(storeRepository.existsByIdAndActiveTrue(storeId)).thenReturn(true);
        when(storeMapRepository.findActiveByStoreId(storeId))
                .thenReturn(Optional.of(storeMap));
    }

    private void givenEmptyElementRepositories(UUID mapId) {
        when(aisleRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of());
        when(shelfBlockRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of());
        when(pointOfInterestRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of());
        when(mapNodeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of());
        when(mapEdgeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of());
    }

    private StoreMap storeMap(UUID storeId, UUID mapId) {
        Store store = org.mockito.Mockito.mock(Store.class);
        when(store.getId()).thenReturn(storeId);
        StoreMap storeMap = org.mockito.Mockito.mock(StoreMap.class);
        when(storeMap.getId()).thenReturn(mapId);
        when(storeMap.getStore()).thenReturn(store);
        when(storeMap.getStatus()).thenReturn(MapStatus.ACTIVE);
        when(storeMap.getVersion()).thenReturn(1);
        when(storeMap.getName()).thenReturn("Store map");
        when(storeMap.getWidth()).thenReturn(new BigDecimal("100.0000"));
        when(storeMap.getHeight()).thenReturn(new BigDecimal("80.0000"));
        when(storeMap.getScaleMetersPerUnit()).thenReturn(BigDecimal.ONE);
        return storeMap;
    }

    private Sector sector(UUID mapId, String name) {
        Sector sector = org.mockito.Mockito.mock(Sector.class);
        when(sector.getId()).thenReturn(UUID.randomUUID());
        StoreMap mapReference = mapReference(mapId);
        when(sector.getStoreMap()).thenReturn(mapReference);
        when(sector.isActive()).thenReturn(true);
        when(sector.getName()).thenReturn(name);
        when(sector.getCode()).thenReturn("SEC-1");
        when(sector.getX()).thenReturn(BigDecimal.ZERO);
        when(sector.getY()).thenReturn(BigDecimal.ZERO);
        when(sector.getWidth()).thenReturn(BigDecimal.TEN);
        when(sector.getHeight()).thenReturn(BigDecimal.TEN);
        when(sector.getRotation()).thenReturn(BigDecimal.ZERO);
        return sector;
    }

    private Aisle aisle(UUID mapId, UUID sectorId) {
        Aisle aisle = org.mockito.Mockito.mock(Aisle.class);
        when(aisle.getId()).thenReturn(UUID.randomUUID());
        StoreMap mapReference = mapReference(mapId);
        when(aisle.getStoreMap()).thenReturn(mapReference);
        when(aisle.getSectorId()).thenReturn(sectorId);
        when(aisle.isActive()).thenReturn(true);
        when(aisle.getCode()).thenReturn("A-1");
        when(aisle.getName()).thenReturn("Aisle");
        when(aisle.getX()).thenReturn(BigDecimal.ZERO);
        when(aisle.getY()).thenReturn(BigDecimal.ZERO);
        when(aisle.getWidth()).thenReturn(BigDecimal.TEN);
        when(aisle.getHeight()).thenReturn(BigDecimal.TEN);
        when(aisle.getRotation()).thenReturn(BigDecimal.ZERO);
        return aisle;
    }

    private ShelfBlock shelfBlock(UUID mapId, UUID sectorId, UUID aisleId) {
        ShelfBlock shelfBlock = org.mockito.Mockito.mock(ShelfBlock.class);
        when(shelfBlock.getId()).thenReturn(UUID.randomUUID());
        StoreMap mapReference = mapReference(mapId);
        when(shelfBlock.getStoreMap()).thenReturn(mapReference);
        when(shelfBlock.getSectorId()).thenReturn(sectorId);
        when(shelfBlock.getAisleId()).thenReturn(aisleId);
        when(shelfBlock.isActive()).thenReturn(true);
        when(shelfBlock.getCode()).thenReturn("S-1");
        when(shelfBlock.getName()).thenReturn("Shelf block");
        when(shelfBlock.getX()).thenReturn(BigDecimal.ZERO);
        when(shelfBlock.getY()).thenReturn(BigDecimal.ZERO);
        when(shelfBlock.getWidth()).thenReturn(BigDecimal.TEN);
        when(shelfBlock.getHeight()).thenReturn(BigDecimal.TEN);
        when(shelfBlock.getRotation()).thenReturn(BigDecimal.ZERO);
        return shelfBlock;
    }

    private PointOfInterest pointOfInterest(UUID mapId, PointOfInterestType type) {
        PointOfInterest pointOfInterest = org.mockito.Mockito.mock(PointOfInterest.class);
        when(pointOfInterest.getId()).thenReturn(UUID.randomUUID());
        StoreMap mapReference = mapReference(mapId);
        when(pointOfInterest.getStoreMap()).thenReturn(mapReference);
        when(pointOfInterest.isActive()).thenReturn(true);
        when(pointOfInterest.getType()).thenReturn(type);
        when(pointOfInterest.getName()).thenReturn("Point");
        when(pointOfInterest.getX()).thenReturn(BigDecimal.ZERO);
        when(pointOfInterest.getY()).thenReturn(BigDecimal.ZERO);
        return pointOfInterest;
    }

    private MapNode node(UUID mapId, MapNodeType type) {
        MapNode node = org.mockito.Mockito.mock(MapNode.class);
        when(node.getId()).thenReturn(UUID.randomUUID());
        StoreMap mapReference = mapReference(mapId);
        when(node.getStoreMap()).thenReturn(mapReference);
        when(node.isActive()).thenReturn(true);
        when(node.getType()).thenReturn(type);
        when(node.getX()).thenReturn(BigDecimal.ZERO);
        when(node.getY()).thenReturn(BigDecimal.ZERO);
        when(node.getLabel()).thenReturn("Node");
        return node;
    }

    private MapEdge edge(UUID mapId, UUID fromNodeId, UUID toNodeId) {
        MapEdge edge = org.mockito.Mockito.mock(MapEdge.class);
        when(edge.getId()).thenReturn(UUID.randomUUID());
        StoreMap mapReference = mapReference(mapId);
        when(edge.getStoreMap()).thenReturn(mapReference);
        when(edge.isActive()).thenReturn(true);
        when(edge.getFromNodeId()).thenReturn(fromNodeId);
        when(edge.getToNodeId()).thenReturn(toNodeId);
        when(edge.getDistanceMeters()).thenReturn(new BigDecimal("10.0000"));
        when(edge.isBidirectional()).thenReturn(true);
        return edge;
    }

    private StoreMap mapReference(UUID mapId) {
        StoreMap map = org.mockito.Mockito.mock(StoreMap.class);
        when(map.getId()).thenReturn(mapId);
        return map;
    }
}
