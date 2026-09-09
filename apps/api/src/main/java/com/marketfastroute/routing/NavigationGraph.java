package com.marketfastroute.routing;

import com.marketfastroute.map.MapEdge;
import com.marketfastroute.map.MapNode;

import java.util.List;
import java.util.UUID;

public record NavigationGraph(
        UUID mapId,
        List<MapNode> nodes,
        List<MapEdge> edges
) {

    public NavigationGraph {
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
    }
}
