package com.marketfastroute.routing;

import com.marketfastroute.map.MapConsistencyException;
import com.marketfastroute.map.MapEdge;
import com.marketfastroute.map.MapNode;
import com.marketfastroute.store.StoreMap;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DijkstraPathFinderTest {

    private final DijkstraPathFinder pathFinder = new DijkstraPathFinder();

    @Test
    void findsTheShortestPathUsingEdgeDistances() {
        MapNode start = node("start");
        MapNode middle = node("middle");
        MapNode destination = node("destination");
        NavigationGraph graph = graph(
                List.of(start, middle, destination),
                List.of(
                        edge(start, middle, "10", false, true),
                        edge(middle, destination, "5", false, true),
                        edge(start, destination, "30", false, true)
                )
        );

        PathResult result = pathFinder.findShortestPath(graph, start.getId(), destination.getId());

        assertEquals(List.of(start, middle, destination), result.nodes());
        assertEquals(new BigDecimal("15"), result.distanceMeters());
    }

    @Test
    void traversesBidirectionalEdgesInBothDirections() {
        MapNode first = node("first");
        MapNode second = node("second");

        PathResult result = pathFinder.findShortestPath(
                graph(List.of(first, second), List.of(edge(first, second, "7", true, true))),
                second.getId(),
                first.getId()
        );

        assertEquals(List.of(second, first), result.nodes());
        assertEquals(new BigDecimal("7"), result.distanceMeters());
    }

    @Test
    void doesNotTraverseAnEdgeAgainstItsDirection() {
        MapNode first = node("first");
        MapNode second = node("second");

        assertThrows(
                RoutePathNotFoundException.class,
                () -> pathFinder.findShortestPath(
                        graph(List.of(first, second), List.of(edge(first, second, "7", false, true))),
                        second.getId(),
                        first.getId()
                )
        );
    }

    @Test
    void ignoresInactiveEdgesAndNodes() {
        MapNode start = node("start");
        MapNode middle = node("middle");
        MapNode destination = node("destination");
        MapNode inactive = node("inactive");
        when(inactive.isActive()).thenReturn(false);

        PathResult result = pathFinder.findShortestPath(
                graph(
                        List.of(start, middle, destination, inactive),
                        List.of(
                                edge(start, middle, "4", false, true),
                                edge(middle, destination, "6", false, true),
                                edge(start, destination, "50", false, false),
                                edge(start, inactive, "1", false, true)
                        )
                ),
                start.getId(),
                destination.getId()
        );

        assertEquals(List.of(start, middle, destination), result.nodes());
        assertEquals(new BigDecimal("10"), result.distanceMeters());
    }

    @Test
    void rejectsAnActiveNodeFromAnotherMap() {
        UUID mapId = UUID.randomUUID();
        StoreMap foreignMap = mock(StoreMap.class);
        when(foreignMap.getId()).thenReturn(UUID.randomUUID());
        MapNode foreignNode = node("foreign");
        when(foreignNode.getStoreMap()).thenReturn(foreignMap);

        assertThrows(
                MapConsistencyException.class,
                () -> pathFinder.findShortestPath(
                        new NavigationGraph(mapId, List.of(foreignNode), List.of()),
                        foreignNode.getId(),
                        foreignNode.getId()
                )
        );
    }

    private NavigationGraph graph(List<MapNode> nodes, List<MapEdge> edges) {
        return new NavigationGraph(UUID.randomUUID(), nodes, edges);
    }

    private MapNode node(String name) {
        MapNode node = mock(MapNode.class);
        when(node.getId()).thenReturn(UUID.nameUUIDFromBytes(name.getBytes()));
        when(node.isActive()).thenReturn(true);
        return node;
    }

    private MapEdge edge(
            MapNode from,
            MapNode to,
            String distance,
            boolean bidirectional,
            boolean active
        ) {
        MapEdge edge = mock(MapEdge.class);
        UUID fromId = from.getId();
        UUID toId = to.getId();
        when(edge.getFromNodeId()).thenReturn(fromId);
        when(edge.getToNodeId()).thenReturn(toId);
        when(edge.getDistanceMeters()).thenReturn(new BigDecimal(distance));
        when(edge.isBidirectional()).thenReturn(bidirectional);
        when(edge.isActive()).thenReturn(active);
        return edge;
    }
}
