package com.marketfastroute.routing;

import com.marketfastroute.map.MapNode;
import com.marketfastroute.product.Product;
import com.marketfastroute.product.ProductLocation;
import com.marketfastroute.product.StoreProduct;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StopOptimizerTest {

    @Test
    void ordersStopsByNearestReachableGraphDistanceWithDeterministicTieBreaks() {
        MapNode start = node("start");
        MapNode productANode = node("product-a-node");
        MapNode productBNode = node("product-b-node");
        UUID mapId = UUID.randomUUID();
        NavigationGraph graph = new NavigationGraph(mapId, List.of(start, productANode, productBNode), List.of());
        ProductLocation productA = location("product-a", "location-a", productANode);
        ProductLocation productB = location("product-b", "location-b", productBNode);

        Map<String, BigDecimal> distances = new HashMap<>();
        distances.put(key(start, productANode), new BigDecimal("12"));
        distances.put(key(start, productBNode), new BigDecimal("4"));
        distances.put(key(productBNode, productANode), new BigDecimal("3"));
        PathFinder pathFinder = distancePathFinder(distances);

        List<ProductLocation> result = new StopOptimizer(pathFinder)
                .optimize(start, List.of(productA, productB), graph);

        assertEquals(List.of(productB, productA), result);
    }

    @Test
    void usesProductIdToBreakEqualDistance() {
        MapNode start = node("start");
        MapNode firstNode = node("first-node");
        MapNode secondNode = node("second-node");
        NavigationGraph graph = new NavigationGraph(UUID.randomUUID(), List.of(start, firstNode, secondNode), List.of());
        ProductLocation productA = location("a-product", "location-z", firstNode);
        ProductLocation productB = location("b-product", "location-a", secondNode);

        PathFinder pathFinder = distancePathFinder(Map.of(
                key(start, firstNode), BigDecimal.TEN,
                key(start, secondNode), BigDecimal.TEN,
                key(firstNode, secondNode), BigDecimal.ONE
        ));

        assertEquals(List.of(productA, productB), new StopOptimizer(pathFinder)
                .optimize(start, List.of(productB, productA), graph));
    }

    private PathFinder distancePathFinder(Map<String, BigDecimal> distances) {
        return (graph, startId, destinationId) -> {
            BigDecimal distance = distances.get(startId + "->" + destinationId);
            if (distance == null) {
                throw new RoutePathNotFoundException(startId, destinationId);
            }
            return new PathResult(List.of(), distance);
        };
    }

    private String key(MapNode from, MapNode to) {
        return from.getId() + "->" + to.getId();
    }

    private MapNode node(String name) {
        MapNode node = mock(MapNode.class);
        when(node.getId()).thenReturn(UUID.nameUUIDFromBytes(name.getBytes()));
        when(node.isActive()).thenReturn(true);
        return node;
    }

    private ProductLocation location(String productName, String locationName, MapNode node) {
        Product product = mock(Product.class);
        when(product.getId()).thenReturn(UUID.nameUUIDFromBytes(productName.getBytes()));
        StoreProduct storeProduct = mock(StoreProduct.class);
        when(storeProduct.getProduct()).thenReturn(product);
        ProductLocation location = mock(ProductLocation.class);
        UUID locationId = UUID.nameUUIDFromBytes(locationName.getBytes());
        UUID nodeId = node.getId();
        when(location.getId()).thenReturn(locationId);
        when(location.getStoreProduct()).thenReturn(storeProduct);
        when(location.getNavigationNodeId()).thenReturn(nodeId);
        return location;
    }
}
