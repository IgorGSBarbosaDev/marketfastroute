package com.marketfastroute.routing;

import com.marketfastroute.map.MapNode;

import java.math.BigDecimal;
import java.util.List;

public record PathResult(
        List<MapNode> nodes,
        BigDecimal distanceMeters
) {

    public PathResult {
        nodes = List.copyOf(nodes);
    }

    public List<MapNode> path() {
        return nodes;
    }
}
