package com.marketfastroute.routing;

import com.marketfastroute.map.MapEdge;
import com.marketfastroute.map.MapNode;
import com.marketfastroute.product.ProductLocation;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RouteComposerTest {

    @Test
    void composesSegmentsWithoutRepeatingJunctionNodesAndSumsDistance() {
        MapNode start = node("start");
        MapNode productNode = node("product");
        MapNode checkout = node("checkout");
        ProductLocation location = mock(ProductLocation.class);
        UUID productNodeId = productNode.getId();
        when(location.getNavigationNodeId()).thenReturn(productNodeId);
        NavigationGraph graph = new NavigationGraph(UUID.randomUUID(), List.of(start, productNode, checkout), List.of());

        PathFinder pathFinder = (ignoredGraph, from, to) -> {
            if (from.equals(start.getId()) && to.equals(productNode.getId())) {
                return new PathResult(List.of(start, productNode), new BigDecimal("8"));
            }
            return new PathResult(List.of(productNode, checkout), new BigDecimal("6"));
        };

        RouteComposer.RouteComposition result = new RouteComposer(pathFinder)
                .compose(start, List.of(location), checkout, graph);

        assertEquals(List.of(start, productNode, checkout), result.path());
        assertEquals(new BigDecimal("14"), result.distanceMeters());
    }

    private MapNode node(String name) {
        MapNode node = mock(MapNode.class);
        when(node.getId()).thenReturn(UUID.nameUUIDFromBytes(name.getBytes()));
        when(node.isActive()).thenReturn(true);
        return node;
    }
}
