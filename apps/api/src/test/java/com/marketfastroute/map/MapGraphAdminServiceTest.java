package com.marketfastroute.map;

import com.marketfastroute.admin.AdminResourceNotFoundException;
import com.marketfastroute.admin.AdminValidationException;
import com.marketfastroute.admin.dto.CreateMapEdgeRequest;
import com.marketfastroute.admin.dto.CreatePointOfInterestRequest;
import com.marketfastroute.store.StoreMap;
import com.marketfastroute.store.StoreMapRepository;
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
class MapGraphAdminServiceTest {

    @Mock
    private StoreMapRepository storeMapRepository;

    @Mock
    private PointOfInterestRepository pointOfInterestRepository;

    @Mock
    private MapNodeRepository mapNodeRepository;

    @Mock
    private MapEdgeRepository mapEdgeRepository;

    private MapGraphAdminService service;

    @BeforeEach
    void setUp() {
        service = new MapGraphAdminService(
                new MapAdminSupport(storeMapRepository), pointOfInterestRepository, mapNodeRepository, mapEdgeRepository);
    }

    @Test
    void rejectsEdgeConnectingNodeToItself() {
        UUID mapId = UUID.randomUUID();
        UUID nodeId = UUID.randomUUID();
        MapNode node = node(nodeId, mapId);
        StoreMap map = map(mapId);
        when(storeMapRepository.findById(mapId)).thenReturn(Optional.of(map));
        when(mapNodeRepository.findByStoreMap_IdAndId(mapId, nodeId)).thenReturn(Optional.of(node));

        assertThrows(AdminValidationException.class, () -> service.createEdge(
                mapId, new CreateMapEdgeRequest(nodeId, nodeId, BigDecimal.ONE, true, true)));
    }

    @Test
    void rejectsPointOfInterestNodeFromAnotherMap() {
        UUID mapId = UUID.randomUUID();
        UUID foreignNodeId = UUID.randomUUID();
        StoreMap map = map(mapId);
        when(storeMapRepository.findById(mapId)).thenReturn(Optional.of(map));
        when(mapNodeRepository.findByStoreMap_IdAndId(mapId, foreignNodeId)).thenReturn(Optional.empty());

        assertThrows(AdminResourceNotFoundException.class, () -> service.createPointOfInterest(
                mapId,
                new CreatePointOfInterestRequest(
                        foreignNodeId, PointOfInterestType.CHECKOUT, "Checkout",
                        BigDecimal.ZERO, BigDecimal.ZERO, true)));
    }

    private StoreMap map(UUID id) {
        return mock(StoreMap.class);
    }

    private MapNode node(UUID id, UUID mapId) {
        MapNode node = new MapNode();
        node.setId(id);
        node.setStoreMap(map(mapId));
        node.setType(MapNodeType.PATH);
        return node;
    }
}
