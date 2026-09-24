package com.marketfastroute.map;

import com.marketfastroute.admin.AdminResourceNotFoundException;
import com.marketfastroute.product.ProductLocationMapValidationRow;
import com.marketfastroute.product.ProductLocationRepository;
import com.marketfastroute.routing.RouteConfigurationException;
import com.marketfastroute.routing.RouteEndpointResolver;
import com.marketfastroute.routing.RoutePointNotFoundException;
import com.marketfastroute.store.StoreMap;
import com.marketfastroute.store.StoreMapRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MapPublicationValidator {

    private static final double BOUNDS_EPSILON = 0.0001;

    private final StoreMapRepository storeMapRepository;
    private final SectorRepository sectorRepository;
    private final AisleRepository aisleRepository;
    private final ShelfBlockRepository shelfBlockRepository;
    private final PointOfInterestRepository pointOfInterestRepository;
    private final MapNodeRepository mapNodeRepository;
    private final MapEdgeRepository mapEdgeRepository;
    private final ProductLocationRepository productLocationRepository;
    private final RouteEndpointResolver endpointResolver;

    public MapPublicationValidator(
            StoreMapRepository storeMapRepository,
            SectorRepository sectorRepository,
            AisleRepository aisleRepository,
            ShelfBlockRepository shelfBlockRepository,
            PointOfInterestRepository pointOfInterestRepository,
            MapNodeRepository mapNodeRepository,
            MapEdgeRepository mapEdgeRepository,
            ProductLocationRepository productLocationRepository,
            RouteEndpointResolver endpointResolver
    ) {
        this.storeMapRepository = storeMapRepository;
        this.sectorRepository = sectorRepository;
        this.aisleRepository = aisleRepository;
        this.shelfBlockRepository = shelfBlockRepository;
        this.pointOfInterestRepository = pointOfInterestRepository;
        this.mapNodeRepository = mapNodeRepository;
        this.mapEdgeRepository = mapEdgeRepository;
        this.productLocationRepository = productLocationRepository;
        this.endpointResolver = endpointResolver;
    }

    public MapPublicationValidation validate(UUID mapId) {
        StoreMap map = storeMapRepository.findById(mapId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Store map"));
        return validate(map);
    }

    public MapPublicationValidation validate(StoreMap map) {
        List<MapPublicationIssue> issues = new ArrayList<>();
        UUID mapId = map.getId();

        if (map.getWidth() == null || map.getWidth().signum() <= 0
                || map.getHeight() == null || map.getHeight().signum() <= 0
                || map.getScaleMetersPerUnit() == null || map.getScaleMetersPerUnit().signum() <= 0) {
            add(issues, "INVALID_MAP_DIMENSIONS", "Map dimensions and scale must be positive", "map", mapId);
            return result(mapId, issues);
        }

        List<Sector> sectors = sectorRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId);
        List<Aisle> aisles = aisleRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId);
        List<ShelfBlock> shelfBlocks = shelfBlockRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId);
        List<PointOfInterest> points = pointOfInterestRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId);
        List<MapNode> nodes = mapNodeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId);
        List<MapEdge> edges = mapEdgeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId);
        List<ProductLocationMapValidationRow> locations = productLocationRepository.findActiveForMapValidation(
                map.getStore().getId(), mapId);

        if (sectors.isEmpty()) {
            add(issues, "NO_ACTIVE_SECTORS", "At least one active sector is required", "sector", null);
        }
        if (aisles.isEmpty()) {
            add(issues, "NO_ACTIVE_AISLES", "At least one active aisle is required", "aisle", null);
        }
        if (nodes.isEmpty()) {
            add(issues, "NO_ACTIVE_NODES", "At least one active navigation node is required", "node", null);
        }
        if (edges.isEmpty()) {
            add(issues, "NO_ACTIVE_EDGES", "At least one active navigation edge is required", "edge", null);
        }
        if (locations.isEmpty()) {
            add(issues, "NO_ACTIVE_PRODUCT_LOCATIONS", "At least one active product location is required", "productLocation", null);
        }

        sectors.forEach(sector -> validateBounds(
                sector.getId(), "sector", sector.getX(), sector.getY(), sector.getWidth(), sector.getHeight(),
                sector.getRotation(), map, issues));
        aisles.forEach(aisle -> validateBounds(
                aisle.getId(), "aisle", aisle.getX(), aisle.getY(), aisle.getWidth(), aisle.getHeight(),
                aisle.getRotation(), map, issues));
        shelfBlocks.forEach(block -> validateBounds(
                block.getId(), "shelfBlock", block.getX(), block.getY(), block.getWidth(), block.getHeight(),
                block.getRotation(), map, issues));
        points.forEach(point -> validatePoint(point.getId(), "pointOfInterest", point.getX(), point.getY(), map, issues));
        nodes.forEach(node -> validatePoint(node.getId(), "node", node.getX(), node.getY(), map, issues));

        Map<UUID, MapNode> activeNodes = new HashMap<>();
        nodes.forEach(node -> activeNodes.put(node.getId(), node));
        for (PointOfInterest point : points) {
            if (point.getNavigationNodeId() != null
                    && (point.getNavigationNode() == null
                    || !activeNodes.containsKey(point.getNavigationNodeId()))) {
                add(issues, "POINT_OF_INTEREST_NODE_INACTIVE",
                        "An active point of interest must reference an active node on this map",
                        "pointOfInterest", point.getId());
            }
        }
        for (ProductLocationMapValidationRow location : locations) {
            if (location.navigationNodeId() == null || !activeNodes.containsKey(location.navigationNodeId())) {
                add(issues, "PRODUCT_LOCATION_NODE_INACTIVE",
                        "An active product location must reference an active node on this map",
                        "productLocation", location.id());
            }
            if ((location.x() == null) != (location.y() == null)) {
                add(issues, "INCOMPLETE_PRODUCT_COORDINATES",
                        "Product location coordinates must both be set or both be empty",
                        "productLocation", location.id());
            } else if (location.x() != null) {
                validatePoint(location.id(), "productLocation", location.x(), location.y(), map, issues);
            }
        }

        MapNode entry = resolveEndpoint(map, PointOfInterestType.ENTRANCE, MapNodeType.ENTRANCE, nodes, issues);
        MapNode checkout = resolveEndpoint(map, PointOfInterestType.CHECKOUT, MapNodeType.CHECKOUT, nodes, issues);
        if (entry != null && checkout != null) {
            validateRouteConnectivity(entry, checkout, selectedRouteLocations(locations), activeNodes, edges, issues);
        }
        return result(mapId, issues);
    }

    public void requirePublishable(StoreMap map) {
        MapPublicationValidation validation = validate(map);
        if (!validation.publishable()) {
            throw new MapPublicationException(validation.issues());
        }
    }

    private MapNode resolveEndpoint(
            StoreMap map,
            PointOfInterestType pointType,
            MapNodeType nodeType,
            List<MapNode> nodes,
            List<MapPublicationIssue> issues
    ) {
        try {
            return endpointResolver.resolve(map.getId(), pointType, nodeType, nodes);
        } catch (RoutePointNotFoundException exception) {
            add(issues, "ROUTE_ENDPOINT_MISSING", "Configure one active " + pointType.name().toLowerCase(),
                    pointType.name().toLowerCase(), null);
        } catch (RouteConfigurationException exception) {
            add(issues, "ROUTE_ENDPOINT_AMBIGUOUS", "Only one active " + pointType.name().toLowerCase()
                            + " or typed node can be used as the route endpoint",
                    pointType.name().toLowerCase(), null);
        }
        return null;
    }

    private void validateRouteConnectivity(
            MapNode entry,
            MapNode checkout,
            List<ProductLocationMapValidationRow> locations,
            Map<UUID, MapNode> activeNodes,
            List<MapEdge> edges,
            List<MapPublicationIssue> issues
    ) {
        Map<UUID, Set<UUID>> adjacency = new HashMap<>();
        activeNodes.keySet().forEach(nodeId -> adjacency.put(nodeId, new LinkedHashSet<>()));
        for (MapEdge edge : edges) {
            if (!activeNodes.containsKey(edge.getFromNodeId()) || !activeNodes.containsKey(edge.getToNodeId())) {
                add(issues, "EDGE_NODE_INACTIVE", "An active edge must connect two active nodes", "edge", edge.getId());
                continue;
            }
            adjacency.get(edge.getFromNodeId()).add(edge.getToNodeId());
            if (edge.isBidirectional()) {
                adjacency.get(edge.getToNodeId()).add(edge.getFromNodeId());
            }
        }

        Set<UUID> routeNodes = new LinkedHashSet<>();
        routeNodes.add(entry.getId());
        routeNodes.add(checkout.getId());
        locations.stream()
                .filter(location -> activeNodes.containsKey(location.navigationNodeId()))
                .map(ProductLocationMapValidationRow::navigationNodeId)
                .forEach(routeNodes::add);

        for (UUID sourceId : routeNodes) {
            Set<UUID> reachable = reachableFrom(sourceId, adjacency);
            List<UUID> unreachableStops = routeNodes.stream()
                    .filter(targetId -> !reachable.contains(targetId))
                    .toList();
            if (!unreachableStops.isEmpty()) {
                add(issues, "ROUTE_STOPS_DISCONNECTED",
                        "Route node " + sourceId + " cannot reach " + unreachableStops.size()
                                + " required route node(s): " + unreachableStops,
                        "node", sourceId);
            }
        }
    }

    private List<ProductLocationMapValidationRow> selectedRouteLocations(List<ProductLocationMapValidationRow> locations) {
        Map<UUID, List<ProductLocationMapValidationRow>> locationsByProduct = new java.util.LinkedHashMap<>();
        for (ProductLocationMapValidationRow location : locations) {
            locationsByProduct.computeIfAbsent(location.productId(), ignored -> new ArrayList<>()).add(location);
        }
        return locationsByProduct.values().stream()
                .map(productLocations -> {
                    List<ProductLocationMapValidationRow> primary = productLocations.stream()
                            .filter(ProductLocationMapValidationRow::primaryLocation)
                            .toList();
                    return primary.isEmpty() ? productLocations.getFirst() : primary.getFirst();
                })
                .toList();
    }

    private Set<UUID> reachableFrom(UUID sourceId, Map<UUID, Set<UUID>> adjacency) {
        Set<UUID> visited = new HashSet<>();
        ArrayDeque<UUID> pending = new ArrayDeque<>();
        pending.add(sourceId);
        while (!pending.isEmpty()) {
            UUID current = pending.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            adjacency.getOrDefault(current, Set.of()).stream()
                    .filter(next -> !visited.contains(next))
                    .forEach(pending::addLast);
        }
        return visited;
    }

    private void validateBounds(
            UUID id,
            String type,
            BigDecimal xValue,
            BigDecimal yValue,
            BigDecimal widthValue,
            BigDecimal heightValue,
            BigDecimal rotationValue,
            StoreMap map,
            List<MapPublicationIssue> issues
    ) {
        if (xValue == null || yValue == null || widthValue == null || heightValue == null
                || rotationValue == null || widthValue.signum() <= 0 || heightValue.signum() <= 0) {
            add(issues, "INVALID_MAP_GEOMETRY", "Map structures need a position and positive dimensions", type, id);
            return;
        }
        double angle = Math.toRadians(rotationValue.doubleValue());
        double halfWidth = widthValue.doubleValue() / 2;
        double halfHeight = heightValue.doubleValue() / 2;
        double extentX = Math.abs(halfWidth * Math.cos(angle)) + Math.abs(halfHeight * Math.sin(angle));
        double extentY = Math.abs(halfWidth * Math.sin(angle)) + Math.abs(halfHeight * Math.cos(angle));
        double centerX = xValue.doubleValue() + halfWidth;
        double centerY = yValue.doubleValue() + halfHeight;
        if (centerX - extentX < -BOUNDS_EPSILON || centerY - extentY < -BOUNDS_EPSILON
                || centerX + extentX > map.getWidth().doubleValue() + BOUNDS_EPSILON
                || centerY + extentY > map.getHeight().doubleValue() + BOUNDS_EPSILON) {
            add(issues, "MAP_ELEMENT_OUT_OF_BOUNDS", "Rotated map element extends beyond map dimensions", type, id);
        }
    }

    private void validatePoint(
            UUID id,
            String type,
            BigDecimal x,
            BigDecimal y,
            StoreMap map,
            List<MapPublicationIssue> issues
    ) {
        if (x == null || y == null || x.signum() < 0 || y.signum() < 0
                || x.compareTo(map.getWidth()) > 0 || y.compareTo(map.getHeight()) > 0) {
            add(issues, "MAP_POINT_OUT_OF_BOUNDS", "Map point must be within map dimensions", type, id);
        }
    }

    private void add(List<MapPublicationIssue> issues, String code, String message, String type, UUID id) {
        issues.add(new MapPublicationIssue(code, message, type, id));
    }

    private MapPublicationValidation result(UUID mapId, Collection<MapPublicationIssue> issues) {
        List<MapPublicationIssue> immutableIssues = List.copyOf(issues);
        return new MapPublicationValidation(mapId, immutableIssues.isEmpty(), immutableIssues);
    }
}
