package com.marketfastroute.map;

import com.marketfastroute.product.ProductLocationMapValidationRow;
import com.marketfastroute.product.ProductLocationRepository;
import com.marketfastroute.routing.RouteEndpointResolver;
import com.marketfastroute.store.Store;
import com.marketfastroute.store.StoreMap;
import com.marketfastroute.store.StoreMapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MapPublicationValidatorTest {

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
    @Mock
    private ProductLocationRepository productLocationRepository;

    private MapPublicationValidator validator;

    @BeforeEach
    void setUp() {
        validator = new MapPublicationValidator(
                storeMapRepository,
                sectorRepository,
                aisleRepository,
                shelfBlockRepository,
                pointOfInterestRepository,
                mapNodeRepository,
                mapEdgeRepository,
                productLocationRepository,
                new RouteEndpointResolver(pointOfInterestRepository));
    }

    @Test
    void acceptsMapWithBoundedGeometryAndConnectedRouteStops() {
        Fixture fixture = fixture(true, false);

        MapPublicationValidation validation = validator.validate(fixture.map());

        assertTrue(validation.publishable());
        assertTrue(validation.issues().isEmpty());
    }

    @Test
    void reportsOutOfBoundsGeometryAndDisconnectedRouteStops() {
        Fixture fixture = fixture(false, true);

        MapPublicationValidation validation = validator.validate(fixture.map());

        assertFalse(validation.publishable());
        assertTrue(validation.issues().stream().anyMatch(issue -> issue.code().equals("MAP_ELEMENT_OUT_OF_BOUNDS")));
        assertTrue(validation.issues().stream().anyMatch(issue -> issue.code().equals("MAP_POINT_OUT_OF_BOUNDS")));
        assertTrue(validation.issues().stream().anyMatch(issue -> issue.code().equals("ROUTE_STOPS_DISCONNECTED")));
    }

    @Test
    void rejectsAmbiguousTypedRouteEndpoints() {
        Fixture fixture = fixture(true, false);
        MapNode secondEntrance = node(UUID.randomUUID(), fixture.map(), MapNodeType.ENTRANCE, 10, 30);
        List<MapNode> nodes = new ArrayList<>(fixture.nodes());
        nodes.add(secondEntrance);
        when(mapNodeRepository.findByStoreMap_IdAndActiveTrueOrderById(fixture.map().getId())).thenReturn(nodes);

        MapPublicationValidation validation = validator.validate(fixture.map());

        assertFalse(validation.publishable());
        assertTrue(validation.issues().stream().anyMatch(issue -> issue.code().equals("ROUTE_ENDPOINT_AMBIGUOUS")));
    }

    @Test
    void rejectsProductLocationsThatReferenceInactiveNodes() {
        Fixture fixture = fixture(true, false);
        ProductLocationMapValidationRow location = fixture.location();
        when(productLocationRepository.findActiveForMapValidation(
                fixture.map().getStore().getId(), fixture.map().getId()))
                .thenReturn(List.of(new ProductLocationMapValidationRow(
                        location.id(), location.productId(), UUID.randomUUID(),
                        location.x(), location.y(), location.primaryLocation())));

        MapPublicationValidation validation = validator.validate(fixture.map());

        assertFalse(validation.publishable());
        assertTrue(validation.issues().stream().anyMatch(issue -> issue.code().equals("PRODUCT_LOCATION_NODE_INACTIVE")));
    }

    @Test
    void rejectsProductLocationsWithOnlyOneCoordinate() {
        Fixture fixture = fixture(true, false);
        ProductLocationMapValidationRow location = fixture.location();
        when(productLocationRepository.findActiveForMapValidation(
                fixture.map().getStore().getId(), fixture.map().getId()))
                .thenReturn(List.of(new ProductLocationMapValidationRow(
                        location.id(), location.productId(), location.navigationNodeId(),
                        BigDecimal.TEN, null, location.primaryLocation())));

        MapPublicationValidation validation = validator.validate(fixture.map());

        assertFalse(validation.publishable());
        assertTrue(validation.issues().stream().anyMatch(issue -> issue.code().equals("INCOMPLETE_PRODUCT_COORDINATES")));
    }

    @Test
    void rejectsActivePointsOfInterestThatReferenceAnInactiveNode() {
        Fixture fixture = fixture(true, false);
        PointOfInterest cart = pointOfInterest(fixture.map(), null, PointOfInterestType.CART);
        when(pointOfInterestRepository.findByStoreMap_IdAndActiveTrueOrderById(fixture.map().getId()))
                .thenReturn(List.of(cart));

        MapPublicationValidation validation = validator.validate(fixture.map());

        assertFalse(validation.publishable());
        assertTrue(validation.issues().stream().anyMatch(issue ->
                issue.code().equals("POINT_OF_INTEREST_NODE_INACTIVE")));
    }

    @Test
    void rejectsDuplicateNavigableEntrancePoints() {
        Fixture fixture = fixture(true, false);
        PointOfInterest first = pointOfInterest(fixture.map(), fixture.nodes().getFirst(), PointOfInterestType.ENTRANCE);
        PointOfInterest second = pointOfInterest(fixture.map(), fixture.nodes().get(1), PointOfInterestType.ENTRANCE);
        when(pointOfInterestRepository.findByStoreMap_IdAndActiveTrueOrderById(fixture.map().getId()))
                .thenReturn(List.of(first, second));
        when(pointOfInterestRepository.findActiveNavigableByMapIdAndType(
                fixture.map().getId(), PointOfInterestType.ENTRANCE)).thenReturn(List.of(first, second));

        MapPublicationValidation validation = validator.validate(fixture.map());

        assertFalse(validation.publishable());
        assertTrue(validation.issues().stream().anyMatch(issue ->
                issue.code().equals("ROUTE_ENDPOINT_AMBIGUOUS")));
    }

    @Test
    void reportsMissingEntryAndCheckoutEndpoints() {
        Fixture fixture = fixture(true, false);
        when(mapNodeRepository.findByStoreMap_IdAndActiveTrueOrderById(fixture.map().getId()))
                .thenReturn(List.of(fixture.nodes().get(1)));

        MapPublicationValidation validation = validator.validate(fixture.map());

        assertFalse(validation.publishable());
        assertTrue(validation.issues().stream().filter(issue ->
                issue.code().equals("ROUTE_ENDPOINT_MISSING")).count() == 2);
    }

    private Fixture fixture(boolean connected, boolean outOfBounds) {
        UUID mapId = UUID.randomUUID();
        UUID storeId = UUID.randomUUID();
        UUID entranceId = UUID.randomUUID();
        UUID productNodeId = UUID.randomUUID();
        UUID checkoutId = UUID.randomUUID();

        Store store = mock(Store.class);
        when(store.getId()).thenReturn(storeId);
        StoreMap map = mock(StoreMap.class);
        when(map.getId()).thenReturn(mapId);
        when(map.getStore()).thenReturn(store);
        when(map.getWidth()).thenReturn(new BigDecimal("100"));
        when(map.getHeight()).thenReturn(new BigDecimal("80"));
        when(map.getScaleMetersPerUnit()).thenReturn(BigDecimal.ONE);

        Sector sector = new Sector();
        sector.setId(UUID.randomUUID());
        sector.setStoreMap(map);
        sector.setX(outOfBounds ? new BigDecimal("85") : BigDecimal.TEN);
        sector.setY(BigDecimal.TEN);
        sector.setWidth(outOfBounds ? new BigDecimal("20") : new BigDecimal("20"));
        sector.setHeight(new BigDecimal("20"));
        sector.setRotation(outOfBounds ? BigDecimal.valueOf(45) : BigDecimal.ZERO);

        Aisle aisle = new Aisle();
        aisle.setId(UUID.randomUUID());
        aisle.setStoreMap(map);
        aisle.setX(new BigDecimal("35"));
        aisle.setY(new BigDecimal("20"));
        aisle.setWidth(new BigDecimal("10"));
        aisle.setHeight(new BigDecimal("35"));
        aisle.setRotation(BigDecimal.ZERO);

        MapNode entrance = node(entranceId, map, MapNodeType.ENTRANCE, 10, 40);
        MapNode productNode = node(productNodeId, map, MapNodeType.PRODUCT_ACCESS, 45, 40);
        MapNode checkout = node(checkoutId, map, MapNodeType.CHECKOUT, 80, 40);
        List<MapNode> nodes = List.of(entrance, productNode, checkout);
        List<MapEdge> edges = List.of(
                edge(map, entrance, productNode, true),
                edge(map, productNode, checkout, connected));

        ProductLocationMapValidationRow location = new ProductLocationMapValidationRow(
                UUID.randomUUID(), UUID.randomUUID(), productNodeId,
                outOfBounds ? new BigDecimal("120") : null,
                outOfBounds ? BigDecimal.TEN : null,
                true);

        when(sectorRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of(sector));
        when(aisleRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of(aisle));
        when(shelfBlockRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of());
        when(pointOfInterestRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of());
        when(pointOfInterestRepository.findActiveNavigableByMapIdAndType(mapId, PointOfInterestType.ENTRANCE))
                .thenReturn(List.of());
        when(pointOfInterestRepository.findActiveNavigableByMapIdAndType(mapId, PointOfInterestType.CHECKOUT))
                .thenReturn(List.of());
        when(mapNodeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(nodes);
        when(mapEdgeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(edges);
        when(productLocationRepository.findActiveForMapValidation(storeId, mapId)).thenReturn(List.of(location));

        return new Fixture(map, nodes, edges, location);
    }

    private MapNode node(UUID id, StoreMap map, MapNodeType type, int x, int y) {
        MapNode node = new MapNode();
        node.setId(id);
        node.setStoreMap(map);
        node.setType(type);
        node.setX(BigDecimal.valueOf(x));
        node.setY(BigDecimal.valueOf(y));
        return node;
    }

    private MapEdge edge(StoreMap map, MapNode from, MapNode to, boolean bidirectional) {
        MapEdge edge = new MapEdge();
        edge.setId(UUID.randomUUID());
        edge.setStoreMap(map);
        edge.setFromNodeId(from.getId());
        edge.setToNodeId(to.getId());
        edge.setBidirectional(bidirectional);
        return edge;
    }

    private PointOfInterest pointOfInterest(StoreMap map, MapNode node, PointOfInterestType type) {
        PointOfInterest point = new PointOfInterest();
        point.setId(UUID.randomUUID());
        point.setStoreMap(map);
        point.setNavigationNodeId(node == null ? UUID.randomUUID() : node.getId());
        point.setNavigationNode(node);
        point.setType(type);
        point.setName(type.name());
        point.setX(BigDecimal.TEN);
        point.setY(BigDecimal.TEN);
        point.setActive(true);
        return point;
    }

    private record Fixture(
            StoreMap map,
            List<MapNode> nodes,
            List<MapEdge> edges,
            ProductLocationMapValidationRow location
    ) {
    }
}
