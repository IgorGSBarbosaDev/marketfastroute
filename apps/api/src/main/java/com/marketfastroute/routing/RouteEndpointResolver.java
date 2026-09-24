package com.marketfastroute.routing;

import com.marketfastroute.map.MapConsistencyException;
import com.marketfastroute.map.MapNode;
import com.marketfastroute.map.MapNodeType;
import com.marketfastroute.map.PointOfInterest;
import com.marketfastroute.map.PointOfInterestRepository;
import com.marketfastroute.map.PointOfInterestType;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
public class RouteEndpointResolver {

    private final PointOfInterestRepository pointOfInterestRepository;

    public RouteEndpointResolver(PointOfInterestRepository pointOfInterestRepository) {
        this.pointOfInterestRepository = pointOfInterestRepository;
    }

    public MapNode resolve(
            UUID mapId,
            PointOfInterestType pointType,
            MapNodeType nodeType,
            List<MapNode> activeNodes
    ) {
        List<MapNode> pointNodes = pointOfInterestRepository
                .findActiveNavigableByMapIdAndType(mapId, pointType).stream()
                .filter(PointOfInterest::isActive)
                .peek(point -> validateMapElement(point.getStoreMap(), mapId, "point of interest"))
                .map(this::navigationNodeFor)
                .filter(Objects::nonNull)
                .filter(MapNode::isActive)
                .peek(node -> validateMapElement(node.getStoreMap(), mapId, "point of interest navigation node"))
                .toList();

        if (pointNodes.size() > 1) {
            throw new RouteConfigurationException(
                    "More than one active navigable " + pointType.name().toLowerCase() + " is configured");
        }
        if (pointNodes.size() == 1) {
            return pointNodes.getFirst();
        }

        List<MapNode> typedNodes = activeNodes.stream()
                .filter(node -> node.getType() == nodeType)
                .toList();
        if (typedNodes.size() > 1) {
            throw new RouteConfigurationException(
                    "More than one active " + nodeType.name().toLowerCase() + " node is configured");
        }
        if (typedNodes.isEmpty()) {
            throw new RoutePointNotFoundException(mapId, pointType.name().toLowerCase());
        }
        return typedNodes.getFirst();
    }

    private MapNode navigationNodeFor(PointOfInterest point) {
        MapNode navigationNode = point.getNavigationNode();
        if (navigationNode != null
                && !Objects.equals(point.getNavigationNodeId(), navigationNode.getId())) {
            throw new MapConsistencyException("Point of interest navigation node does not match navigation_node_id");
        }
        return navigationNode;
    }

    private void validateMapElement(com.marketfastroute.store.StoreMap map, UUID mapId, String elementName) {
        if (map == null || !Objects.equals(map.getId(), mapId)) {
            throw new MapConsistencyException("Active map contains a " + elementName + " from another map");
        }
    }
}
