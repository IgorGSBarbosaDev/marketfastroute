package com.marketfastroute.routing;

import com.marketfastroute.map.MapEdge;
import com.marketfastroute.map.MapNode;
import com.marketfastroute.map.MapConsistencyException;
import com.marketfastroute.store.StoreMap;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.UUID;

@Component
public class DijkstraPathFinder implements PathFinder {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    @Override
    public PathResult findShortestPath(
            NavigationGraph graph,
            UUID startNodeId,
            UUID destinationNodeId
    ) {
        Map<UUID, MapNode> activeNodes = activeNodesById(graph);
        if (!activeNodes.containsKey(startNodeId) || !activeNodes.containsKey(destinationNodeId)) {
            throw new RoutePathNotFoundException(startNodeId, destinationNodeId);
        }

        Map<UUID, List<Traversal>> adjacency = buildAdjacency(graph, activeNodes);
        Map<UUID, BigDecimal> distances = new HashMap<>();
        Map<UUID, UUID> previousNodes = new HashMap<>();
        PriorityQueue<NodeDistance> queue = new PriorityQueue<>(
                Comparator.comparing(NodeDistance::distance)
                        .thenComparing(nodeDistance -> nodeDistance.nodeId().toString())
        );

        distances.put(startNodeId, ZERO);
        queue.add(new NodeDistance(startNodeId, ZERO));

        while (!queue.isEmpty()) {
            NodeDistance current = queue.poll();
            if (current.distance().compareTo(distances.get(current.nodeId())) != 0) {
                continue;
            }
            if (current.nodeId().equals(destinationNodeId)) {
                break;
            }

            for (Traversal traversal : adjacency.getOrDefault(current.nodeId(), List.of())) {
                BigDecimal candidateDistance = current.distance().add(traversal.distance());
                BigDecimal knownDistance = distances.get(traversal.destinationNodeId());
                if (knownDistance == null || candidateDistance.compareTo(knownDistance) < 0) {
                    distances.put(traversal.destinationNodeId(), candidateDistance);
                    previousNodes.put(traversal.destinationNodeId(), current.nodeId());
                    queue.add(new NodeDistance(traversal.destinationNodeId(), candidateDistance));
                }
            }
        }

        BigDecimal totalDistance = distances.get(destinationNodeId);
        if (totalDistance == null) {
            throw new RoutePathNotFoundException(startNodeId, destinationNodeId);
        }

        return new PathResult(reconstructPath(activeNodes, previousNodes, startNodeId, destinationNodeId), totalDistance);
    }

    private Map<UUID, MapNode> activeNodesById(NavigationGraph graph) {
        if (graph == null || graph.mapId() == null) {
            throw new MapConsistencyException("Navigation graph must have a map identifier");
        }

        Map<UUID, MapNode> activeNodes = new HashMap<>();
        for (MapNode node : graph.nodes()) {
            if (node == null || !node.isActive()) {
                continue;
            }
            validateMapReference(node.getStoreMap(), graph.mapId(), "navigation node");
            if (node.getId() == null) {
                throw new MapConsistencyException("Active navigation node has no identifier");
            }
            if (activeNodes.put(node.getId(), node) != null) {
                throw new MapConsistencyException("Navigation graph contains duplicate node: " + node.getId());
            }
        }
        return activeNodes;
    }

    private Map<UUID, List<Traversal>> buildAdjacency(
            NavigationGraph graph,
            Map<UUID, MapNode> activeNodes
    ) {
        Map<UUID, List<Traversal>> adjacency = new HashMap<>();
        for (MapEdge edge : graph.edges()) {
            if (edge == null || !edge.isActive()) {
                continue;
            }
            validateMapReference(edge.getStoreMap(), graph.mapId(), "map edge");
            if (edge.getFromNodeId() == null || edge.getToNodeId() == null
                    || edge.getDistanceMeters() == null
                    || edge.getDistanceMeters().compareTo(ZERO) <= 0) {
                throw new MapConsistencyException("Active map edge has invalid navigation data");
            }

            // Edges incident to inactive nodes are not part of the active graph.
            if (!activeNodes.containsKey(edge.getFromNodeId())
                    || !activeNodes.containsKey(edge.getToNodeId())) {
                continue;
            }

            adjacency.computeIfAbsent(edge.getFromNodeId(), ignored -> new ArrayList<>())
                    .add(new Traversal(edge.getToNodeId(), edge.getDistanceMeters()));
            if (edge.isBidirectional()) {
                adjacency.computeIfAbsent(edge.getToNodeId(), ignored -> new ArrayList<>())
                        .add(new Traversal(edge.getFromNodeId(), edge.getDistanceMeters()));
            }
        }
        adjacency.values().forEach(traversals -> traversals.sort(
                Comparator.comparing(Traversal::destinationNodeId)
        ));
        return adjacency;
    }

    private List<MapNode> reconstructPath(
            Map<UUID, MapNode> nodes,
            Map<UUID, UUID> previousNodes,
            UUID startNodeId,
            UUID destinationNodeId
    ) {
        List<MapNode> reversedPath = new ArrayList<>();
        UUID currentNodeId = destinationNodeId;
        while (currentNodeId != null) {
            reversedPath.add(nodes.get(currentNodeId));
            if (currentNodeId.equals(startNodeId)) {
                break;
            }
            currentNodeId = previousNodes.get(currentNodeId);
        }
        if (!reversedPath.getLast().getId().equals(startNodeId)) {
            throw new RoutePathNotFoundException(startNodeId, destinationNodeId);
        }
        java.util.Collections.reverse(reversedPath);
        return reversedPath;
    }

    private void validateMapReference(StoreMap storeMap, UUID mapId, String elementType) {
        if (storeMap != null && !mapId.equals(storeMap.getId())) {
            throw new MapConsistencyException("Map element belongs to another map: " + elementType);
        }
    }

    private record Traversal(UUID destinationNodeId, BigDecimal distance) {
    }

    private record NodeDistance(UUID nodeId, BigDecimal distance) {
    }
}
