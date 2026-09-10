package com.marketfastroute.routing;

import com.marketfastroute.map.MapEdge;
import com.marketfastroute.map.MapEdgeRepository;
import com.marketfastroute.map.MapNode;
import com.marketfastroute.map.MapNodeRepository;
import com.marketfastroute.map.MapNodeType;
import com.marketfastroute.map.MapStatus;
import com.marketfastroute.map.ActiveStoreMapResolver;
import com.marketfastroute.map.PointOfInterest;
import com.marketfastroute.map.PointOfInterestRepository;
import com.marketfastroute.map.PointOfInterestType;
import com.marketfastroute.product.Product;
import com.marketfastroute.product.ProductLocation;
import com.marketfastroute.product.ProductLocationNotFoundException;
import com.marketfastroute.product.ProductLocationRepository;
import com.marketfastroute.product.ProductNotFoundException;
import com.marketfastroute.product.StoreProduct;
import com.marketfastroute.product.StoreProductRepository;
import com.marketfastroute.store.Store;
import com.marketfastroute.store.StoreMap;
import com.marketfastroute.store.StoreMapRepository;
import com.marketfastroute.store.StoreNotFoundException;
import com.marketfastroute.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RouteServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreMapRepository storeMapRepository;

    @Mock
    private StoreProductRepository storeProductRepository;

    @Mock
    private ProductLocationRepository productLocationRepository;

    @Mock
    private MapNodeRepository mapNodeRepository;

    @Mock
    private MapEdgeRepository mapEdgeRepository;

    @Mock
    private PointOfInterestRepository pointOfInterestRepository;

    private RouteService routeService;

    @BeforeEach
    void setUp() {
        DijkstraPathFinder pathFinder = new DijkstraPathFinder();
        routeService = new RouteService(
                new ActiveStoreMapResolver(storeRepository, storeMapRepository),
                storeProductRepository,
                productLocationRepository,
                mapNodeRepository,
                mapEdgeRepository,
                pointOfInterestRepository,
                new StopOptimizer(pathFinder),
                new RouteComposer(pathFinder)
        );
    }

    @Test
    void calculatesAnOrderedRouteFromEntranceThroughProductsToCheckout() {
        UUID storeId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        Store store = store(storeId);
        StoreMap map = activeMap(store, mapId);
        MapNode entrance = node("entrance", MapNodeType.ENTRANCE, map, 0, 0);
        MapNode productANode = node("product-a-node", MapNodeType.PRODUCT_ACCESS, map, 1, 0);
        MapNode productBNode = node("product-b-node", MapNodeType.PRODUCT_ACCESS, map, 2, 0);
        MapNode checkout = node("checkout", MapNodeType.CHECKOUT, map, 3, 0);
        List<MapNode> nodes = List.of(entrance, productANode, productBNode, checkout);
        List<MapEdge> edges = List.of(
                edge(entrance, productANode, 5, map),
                edge(productANode, productBNode, 5, map),
                edge(productBNode, checkout, 5, map)
        );
        ProductWithAvailability productA = product(storeId, "product-a", "Milk");
        ProductWithAvailability productB = product(storeId, "product-b", "Bread");
        ProductLocation locationA = location(storeId, map, productA.storeProduct(), productANode, "location-a");
        ProductLocation locationB = location(storeId, map, productB.storeProduct(), productBNode, "location-b");
        PointOfInterest entrancePoint = point(map, entrance);
        PointOfInterest checkoutPoint = point(map, checkout);

        when(storeRepository.existsByIdAndActiveTrue(storeId)).thenReturn(true);
        when(storeMapRepository.findActiveByStoreId(storeId)).thenReturn(Optional.of(map));
        when(mapNodeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(nodes);
        when(mapEdgeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(edges);
        when(storeProductRepository.findAvailableByStoreIdAndProductIdIn(
                storeId, List.of(productB.productId(), productA.productId())))
                .thenReturn(List.of(productA.storeProduct(), productB.storeProduct()));
        when(productLocationRepository.findActiveByStoreIdAndMapIdAndProductIdIn(
                storeId, mapId, List.of(productB.productId(), productA.productId())))
                .thenReturn(List.of(locationA, locationB));
        when(pointOfInterestRepository.findActiveNavigableByMapIdAndType(mapId, PointOfInterestType.ENTRANCE))
                .thenReturn(List.of(entrancePoint));
        when(pointOfInterestRepository.findActiveNavigableByMapIdAndType(mapId, PointOfInterestType.CHECKOUT))
                .thenReturn(List.of(checkoutPoint));

        RouteResponse result = routeService.calculate(new RouteRequest(
                storeId,
                List.of(productB.productId(), productA.productId())
        ));

        assertEquals(storeId, result.storeId());
        assertEquals(mapId, result.mapId());
        assertEquals(List.of(productA.productId(), productB.productId()),
                result.orderedStops().stream().map(RouteStopResponse::productId).toList());
        assertEquals(List.of(
                        entrance.getId(), productANode.getId(), productBNode.getId(), checkout.getId()
                ), result.path().stream().map(RoutePathNodeResponse::nodeId).toList());
        assertEquals(new BigDecimal("15"), result.distanceMeters());
        assertEquals(List.of(1, 2), result.orderedStops().stream().map(RouteStopResponse::order).toList());
        verify(storeProductRepository).findAvailableByStoreIdAndProductIdIn(
                storeId, List.of(productB.productId(), productA.productId()));
        verify(productLocationRepository).findActiveByStoreIdAndMapIdAndProductIdIn(
                storeId, mapId, List.of(productB.productId(), productA.productId()));
    }

    @Test
    void rejectsAStoreThatDoesNotExistBeforeLoadingRouteData() {
        UUID storeId = UUID.randomUUID();
        when(storeRepository.existsByIdAndActiveTrue(storeId)).thenReturn(false);

        assertThrows(StoreNotFoundException.class,
                () -> routeService.calculate(new RouteRequest(storeId, List.of(UUID.randomUUID()))));

        verifyNoInteractions(storeMapRepository, storeProductRepository, productLocationRepository,
                mapNodeRepository, mapEdgeRepository, pointOfInterestRepository);
    }

    @Test
    void rejectsAProductUnavailableInTheStore() {
        UUID storeId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        StoreMap map = activeMap(store(storeId), mapId);
        when(storeRepository.existsByIdAndActiveTrue(storeId)).thenReturn(true);
        when(storeMapRepository.findActiveByStoreId(storeId)).thenReturn(Optional.of(map));
        when(mapNodeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of());
        when(mapEdgeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of());

        UUID productId = UUID.randomUUID();
        when(storeProductRepository.findAvailableByStoreIdAndProductIdIn(storeId, List.of(productId)))
                .thenReturn(List.of());

        assertThrows(ProductNotFoundException.class,
                () -> routeService.calculate(new RouteRequest(storeId, List.of(productId))));
        verify(productLocationRepository, never())
                .findActiveByStoreIdAndMapIdAndProductIdIn(storeId, mapId, List.of(productId));
    }

    @Test
    void rejectsAProductWithoutAnActivePrimaryLocationOnTheActiveMap() {
        UUID storeId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        StoreMap map = activeMap(store(storeId), mapId);
        ProductWithAvailability product = product(storeId, "product", "Product");
        when(storeRepository.existsByIdAndActiveTrue(storeId)).thenReturn(true);
        when(storeMapRepository.findActiveByStoreId(storeId)).thenReturn(Optional.of(map));
        when(mapNodeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of());
        when(mapEdgeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of());
        when(storeProductRepository.findAvailableByStoreIdAndProductIdIn(storeId, List.of(product.productId())))
                .thenReturn(List.of(product.storeProduct()));
        when(productLocationRepository.findActiveByStoreIdAndMapIdAndProductIdIn(
                storeId, mapId, List.of(product.productId())))
                .thenReturn(List.of());

        assertThrows(ProductLocationNotFoundException.class,
                () -> routeService.calculate(new RouteRequest(storeId, List.of(product.productId()))));
    }

    @Test
    void rejectsAnActiveGraphElementFromAnotherMap() {
        UUID storeId = UUID.randomUUID();
        UUID mapId = UUID.randomUUID();
        StoreMap map = activeMap(store(storeId), mapId);
        StoreMap otherMap = mock(StoreMap.class);
        when(otherMap.getId()).thenReturn(UUID.randomUUID());
        MapNode foreignNode = node("foreign", MapNodeType.PATH, otherMap, 0, 0);
        when(storeRepository.existsByIdAndActiveTrue(storeId)).thenReturn(true);
        when(storeMapRepository.findActiveByStoreId(storeId)).thenReturn(Optional.of(map));
        when(mapNodeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId)).thenReturn(List.of(foreignNode));

        assertThrows(com.marketfastroute.map.MapConsistencyException.class,
                () -> routeService.calculate(new RouteRequest(storeId, List.of(UUID.randomUUID()))));
        verifyNoInteractions(mapEdgeRepository, storeProductRepository, productLocationRepository,
                pointOfInterestRepository);
    }

    private Store store(UUID storeId) {
        Store store = mock(Store.class);
        when(store.getId()).thenReturn(storeId);
        return store;
    }

    private StoreMap activeMap(Store store, UUID mapId) {
        StoreMap map = mock(StoreMap.class);
        when(map.getId()).thenReturn(mapId);
        when(map.getStore()).thenReturn(store);
        when(map.getStatus()).thenReturn(MapStatus.ACTIVE);
        return map;
    }

    private MapNode node(String name, MapNodeType type, StoreMap map, int x, int y) {
        MapNode node = mock(MapNode.class);
        when(node.getId()).thenReturn(UUID.nameUUIDFromBytes(name.getBytes()));
        when(node.getStoreMap()).thenReturn(map);
        when(node.getType()).thenReturn(type);
        when(node.getX()).thenReturn(BigDecimal.valueOf(x));
        when(node.getY()).thenReturn(BigDecimal.valueOf(y));
        when(node.isActive()).thenReturn(true);
        return node;
    }

    private MapEdge edge(MapNode from, MapNode to, int distance, StoreMap map) {
        MapEdge edge = mock(MapEdge.class);
        when(edge.getStoreMap()).thenReturn(map);
        UUID fromId = from.getId();
        UUID toId = to.getId();
        when(edge.getFromNodeId()).thenReturn(fromId);
        when(edge.getToNodeId()).thenReturn(toId);
        when(edge.getDistanceMeters()).thenReturn(BigDecimal.valueOf(distance));
        when(edge.isBidirectional()).thenReturn(false);
        when(edge.isActive()).thenReturn(true);
        return edge;
    }

    private PointOfInterest point(StoreMap map, MapNode node) {
        PointOfInterest point = mock(PointOfInterest.class);
        UUID navigationNodeId = node.getId();
        when(point.getStoreMap()).thenReturn(map);
        when(point.getNavigationNodeId()).thenReturn(navigationNodeId);
        when(point.getNavigationNode()).thenReturn(node);
        when(point.isActive()).thenReturn(true);
        return point;
    }

    private ProductWithAvailability product(UUID storeId, String name, String displayName) {
        UUID productId = UUID.nameUUIDFromBytes(name.getBytes());
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(productId);
        when(product.getName()).thenReturn(displayName);
        when(product.isActive()).thenReturn(true);
        StoreProduct storeProduct = mock(StoreProduct.class);
        when(storeProduct.getId()).thenReturn(UUID.nameUUIDFromBytes((name + "-store").getBytes()));
        Store store = store(storeId);
        when(storeProduct.getStore()).thenReturn(store);
        when(storeProduct.getProduct()).thenReturn(product);
        when(storeProduct.isActive()).thenReturn(true);
        return new ProductWithAvailability(productId, storeProduct);
    }

    private ProductLocation location(
            UUID storeId,
            StoreMap map,
            StoreProduct storeProduct,
            MapNode navigationNode,
            String name
    ) {
        ProductLocation location = mock(ProductLocation.class);
        UUID locationId = UUID.nameUUIDFromBytes(name.getBytes());
        UUID storeProductId = storeProduct.getId();
        UUID mapId = map.getId();
        UUID navigationNodeId = navigationNode.getId();
        when(location.getId()).thenReturn(locationId);
        when(location.getStoreId()).thenReturn(storeId);
        when(location.getStoreProductId()).thenReturn(storeProductId);
        when(location.getStoreProduct()).thenReturn(storeProduct);
        when(location.getMapId()).thenReturn(mapId);
        when(location.getStoreMap()).thenReturn(map);
        when(location.getNavigationNodeId()).thenReturn(navigationNodeId);
        when(location.getNavigationNode()).thenReturn(navigationNode);
        when(location.isPrimaryLocation()).thenReturn(true);
        when(location.isActive()).thenReturn(true);
        return location;
    }

    private record ProductWithAvailability(UUID productId, StoreProduct storeProduct) {
    }
}
