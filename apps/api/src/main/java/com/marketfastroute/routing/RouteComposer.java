package com.marketfastroute.routing;

import com.marketfastroute.map.MapNode;
import com.marketfastroute.product.ProductLocation;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
public class RouteComposer {

    private final PathFinder pathFinder;

    public RouteComposer(PathFinder pathFinder) {
        this.pathFinder = pathFinder;
    }

    public RouteComposition compose(
            MapNode startNode,
            List<ProductLocation> orderedLocations,
            MapNode destinationNode,
            NavigationGraph graph
    ) {
        List<MapNode> routePath = new ArrayList<>();
        BigDecimal totalDistance = BigDecimal.ZERO;
        java.util.UUID currentNodeId = startNode.getId();
        routePath.add(startNode);

        for (ProductLocation location : orderedLocations) {
            PathResult segment = pathFinder.findShortestPath(
                    graph,
                    currentNodeId,
                    location.getNavigationNodeId()
            );
            appendSegment(routePath, segment.nodes());
            totalDistance = totalDistance.add(segment.distanceMeters());
            currentNodeId = location.getNavigationNodeId();
        }

        PathResult finalSegment = pathFinder.findShortestPath(
                graph,
                currentNodeId,
                destinationNode.getId()
        );
        appendSegment(routePath, finalSegment.nodes());
        totalDistance = totalDistance.add(finalSegment.distanceMeters());

        return new RouteComposition(routePath, totalDistance);
    }

    private void appendSegment(List<MapNode> routePath, List<MapNode> segment) {
        int startIndex = !routePath.isEmpty()
                && routePath.getLast().getId().equals(segment.getFirst().getId())
                ? 1
                : 0;
        routePath.addAll(segment.subList(startIndex, segment.size()));
    }

    public record RouteComposition(List<MapNode> path, BigDecimal distanceMeters) {

        public RouteComposition {
            path = List.copyOf(path);
        }
    }
}
