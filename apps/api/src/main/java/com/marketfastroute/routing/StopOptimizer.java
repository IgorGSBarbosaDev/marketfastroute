package com.marketfastroute.routing;

import com.marketfastroute.product.ProductLocation;
import com.marketfastroute.map.MapNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Greedy nearest-neighbour ordering based on graph shortest-path distance.
 * It is predictable for the MVP and does not attempt an exact TSP solution.
 */
@Component
public class StopOptimizer {

    private final PathFinder pathFinder;

    public StopOptimizer(PathFinder pathFinder) {
        this.pathFinder = pathFinder;
    }

    public List<ProductLocation> optimize(
            MapNode startNode,
            List<ProductLocation> locations,
            NavigationGraph graph
    ) {
        List<ProductLocation> remaining = new ArrayList<>(locations);
        remaining.sort(productLocationComparator());

        List<ProductLocation> ordered = new ArrayList<>(remaining.size());
        UUID currentNodeId = startNode.getId();
        while (!remaining.isEmpty()) {
            ProductLocation next = findNearestReachable(currentNodeId, remaining, graph);
            ordered.add(next);
            remaining.remove(next);
            currentNodeId = next.getNavigationNodeId();
        }
        return List.copyOf(ordered);
    }

    private ProductLocation findNearestReachable(
            UUID currentNodeId,
            List<ProductLocation> remaining,
            NavigationGraph graph
    ) {
        ProductLocation nearest = null;
        PathResult nearestPath = null;
        for (ProductLocation candidate : remaining) {
            try {
                PathResult candidatePath = pathFinder.findShortestPath(
                        graph,
                        currentNodeId,
                        candidate.getNavigationNodeId()
                );
                if (nearestPath == null
                        || candidatePath.distanceMeters().compareTo(nearestPath.distanceMeters()) < 0
                        || (candidatePath.distanceMeters().compareTo(nearestPath.distanceMeters()) == 0
                        && productLocationComparator().compare(candidate, nearest) < 0)) {
                    nearest = candidate;
                    nearestPath = candidatePath;
                }
            } catch (RoutePathNotFoundException ignored) {
                // A candidate may be reachable after another stop. The final
                // composition still validates every segment of the route.
            }
        }

        if (nearest == null) {
            ProductLocation first = remaining.getFirst();
            throw new RoutePathNotFoundException(currentNodeId, first.getNavigationNodeId());
        }
        return nearest;
    }

    private Comparator<ProductLocation> productLocationComparator() {
        return Comparator.comparing(this::productId)
                .thenComparing(ProductLocation::getId);
    }

    private UUID productId(ProductLocation location) {
        return location.getStoreProduct().getProduct().getId();
    }
}
