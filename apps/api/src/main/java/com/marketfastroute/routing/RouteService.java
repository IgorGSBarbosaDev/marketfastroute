package com.marketfastroute.routing;

import com.marketfastroute.map.MapConsistencyException;
import com.marketfastroute.map.MapEdge;
import com.marketfastroute.map.MapNode;
import com.marketfastroute.map.MapNodeType;
import com.marketfastroute.map.ActiveStoreMapResolver;
import com.marketfastroute.map.PointOfInterest;
import com.marketfastroute.map.PointOfInterestRepository;
import com.marketfastroute.map.PointOfInterestType;
import com.marketfastroute.map.MapEdgeRepository;
import com.marketfastroute.map.MapNodeRepository;
import com.marketfastroute.product.Product;
import com.marketfastroute.product.ProductLocation;
import com.marketfastroute.product.ProductLocationConsistencyException;
import com.marketfastroute.product.ProductLocationNotFoundException;
import com.marketfastroute.product.ProductNotFoundException;
import com.marketfastroute.product.ProductLocationRepository;
import com.marketfastroute.product.StoreProduct;
import com.marketfastroute.product.StoreProductRepository;
import com.marketfastroute.store.StoreMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
@Transactional(readOnly = true)
public class RouteService {

    private final ActiveStoreMapResolver activeStoreMapResolver;
    private final StoreProductRepository storeProductRepository;
    private final ProductLocationRepository productLocationRepository;
    private final MapNodeRepository mapNodeRepository;
    private final MapEdgeRepository mapEdgeRepository;
    private final PointOfInterestRepository pointOfInterestRepository;
    private final StopOptimizer stopOptimizer;
    private final RouteComposer routeComposer;

    public RouteService(
            ActiveStoreMapResolver activeStoreMapResolver,
            StoreProductRepository storeProductRepository,
            ProductLocationRepository productLocationRepository,
            MapNodeRepository mapNodeRepository,
            MapEdgeRepository mapEdgeRepository,
            PointOfInterestRepository pointOfInterestRepository,
            StopOptimizer stopOptimizer,
            RouteComposer routeComposer
    ) {
        this.activeStoreMapResolver = activeStoreMapResolver;
        this.storeProductRepository = storeProductRepository;
        this.productLocationRepository = productLocationRepository;
        this.mapNodeRepository = mapNodeRepository;
        this.mapEdgeRepository = mapEdgeRepository;
        this.pointOfInterestRepository = pointOfInterestRepository;
        this.stopOptimizer = stopOptimizer;
        this.routeComposer = routeComposer;
    }

    public RouteResponse calculate(RouteRequest request) {
        validateRequest(request);
        UUID storeId = request.storeId();
        List<UUID> productIds = request.productIds().stream().distinct().toList();

        StoreMap storeMap = activeStoreMapResolver.resolve(storeId);
        UUID mapId = requireIdentifier(storeMap.getId(), "Active map has no identifier");

        NavigationGraph graph = loadGraph(mapId);
        Map<UUID, StoreProduct> availableProducts = loadAvailableProducts(storeId, productIds);
        List<ProductLocation> locations = loadValidProductLocations(
                storeId,
                mapId,
                productIds,
                availableProducts,
                graph
        );
        MapNode startNode = findEndpoint(
                mapId,
                PointOfInterestType.ENTRANCE,
                MapNodeType.ENTRANCE,
                graph.nodes()
        );
        MapNode destinationNode = findEndpoint(
                mapId,
                PointOfInterestType.CHECKOUT,
                MapNodeType.CHECKOUT,
                graph.nodes()
        );

        List<ProductLocation> orderedLocations = stopOptimizer.optimize(startNode, locations, graph);
        RouteComposer.RouteComposition composition = routeComposer.compose(
                startNode,
                orderedLocations,
                destinationNode,
                graph
        );

        return toResponse(storeId, mapId, orderedLocations, composition);
    }

    private void validateRequest(RouteRequest request) {
        if (request == null || request.storeId() == null
                || request.productIds() == null || request.productIds().isEmpty()
                || request.productIds().stream().anyMatch(Objects::isNull)) {
            throw new InvalidRouteRequestException("storeId and at least one productId are required");
        }
    }

    private NavigationGraph loadGraph(UUID mapId) {
        List<MapNode> nodes = mapNodeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId).stream()
                .filter(MapNode::isActive)
                .peek(node -> validateMapElement(node.getStoreMap(), mapId, "navigation node"))
                .toList();
        List<MapEdge> edges = mapEdgeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId).stream()
                .filter(MapEdge::isActive)
                .peek(edge -> validateMapElement(edge.getStoreMap(), mapId, "map edge"))
                .toList();

        return new NavigationGraph(mapId, nodes, edges);
    }

    private Map<UUID, StoreProduct> loadAvailableProducts(UUID storeId, List<UUID> productIds) {
        Map<UUID, StoreProduct> availableProducts = new HashMap<>();
        for (StoreProduct storeProduct : storeProductRepository
                .findAvailableByStoreIdAndProductIdIn(storeId, productIds)) {
            if (storeProduct == null || !storeProduct.isActive()
                    || storeProduct.getStore() == null
                    || !Objects.equals(storeProduct.getStore().getId(), storeId)
                    || storeProduct.getProduct() == null
                    || !storeProduct.getProduct().isActive()
                    || storeProduct.getProduct().getId() == null) {
                continue;
            }
            availableProducts.put(storeProduct.getProduct().getId(), storeProduct);
        }

        for (UUID productId : productIds) {
            if (!availableProducts.containsKey(productId)) {
                throw new ProductNotFoundException(storeId, productId);
            }
        }
        return availableProducts;
    }

    private List<ProductLocation> loadValidProductLocations(
            UUID storeId,
            UUID mapId,
            List<UUID> productIds,
            Map<UUID, StoreProduct> availableProducts,
            NavigationGraph graph
    ) {
        Map<UUID, List<ProductLocation>> locationsByProduct = new HashMap<>();
        for (ProductLocation location : productLocationRepository
                .findActiveByStoreIdAndMapIdAndProductIdIn(storeId, mapId, productIds)) {
            if (location == null || !location.isActive()) {
                continue;
            }
            validateProductLocation(location, storeId, mapId, availableProducts, graph);
            UUID productId = location.getStoreProduct().getProduct().getId();
            locationsByProduct.computeIfAbsent(productId, ignored -> new ArrayList<>()).add(location);
        }

        Map<UUID, ProductLocation> selectedLocations = new HashMap<>();
        for (UUID productId : productIds) {
            List<ProductLocation> productLocations = locationsByProduct.getOrDefault(productId, List.of());
            if (productLocations.isEmpty()) {
                throw new ProductLocationNotFoundException(storeId, productId);
            }
            List<ProductLocation> primaryLocations = productLocations.stream()
                    .filter(ProductLocation::isPrimaryLocation)
                    .toList();
            if (primaryLocations.size() > 1) {
                throw new ProductLocationConsistencyException(
                        primaryLocations.getFirst().getId(),
                        "more than one primary location exists for the product on the active map"
                );
            }
            selectedLocations.put(productId, primaryLocations.isEmpty()
                    ? productLocations.getFirst()
                    : primaryLocations.getFirst());
        }

        return productIds.stream().map(selectedLocations::get).toList();
    }

    private void validateProductLocation(
            ProductLocation location,
            UUID storeId,
            UUID mapId,
            Map<UUID, StoreProduct> availableProducts,
            NavigationGraph graph
    ) {
        StoreProduct storeProduct = location.getStoreProduct();
        Product product = storeProduct == null ? null : storeProduct.getProduct();
        if (!Objects.equals(location.getStoreId(), storeId)
                || storeProduct == null
                || !Objects.equals(location.getStoreProductId(), storeProduct.getId())
                || storeProduct.getStore() == null
                || !Objects.equals(storeProduct.getStore().getId(), storeId)
                || product == null
                || product.getId() == null
                || !availableProducts.containsKey(product.getId())
                || !Objects.equals(location.getMapId(), mapId)) {
            inconsistentLocation(location, "product location does not belong to the requested store and product");
        }

        StoreMap locationMap = location.getStoreMap();
        if (locationMap == null || !Objects.equals(locationMap.getId(), mapId)
                || locationMap.getStore() == null
                || !Objects.equals(locationMap.getStore().getId(), storeId)) {
            inconsistentLocation(location, "product location map does not belong to the requested store");
        }

        MapNode navigationNode = location.getNavigationNode();
        if (navigationNode == null
                || !Objects.equals(location.getNavigationNodeId(), navigationNode.getId())) {
            inconsistentLocation(location, "product location navigation node does not match navigation_node_id");
        }
        validateMapElement(navigationNode.getStoreMap(), mapId, "product location navigation node");
        if (!navigationNode.isActive() || !graph.nodes().stream()
                .anyMatch(node -> Objects.equals(node.getId(), navigationNode.getId()))) {
            throw new ProductLocationNotFoundException(storeId, product.getId());
        }
    }

    private MapNode findEndpoint(
            UUID mapId,
            PointOfInterestType pointType,
            MapNodeType nodeType,
            List<MapNode> activeNodes
    ) {
        List<PointOfInterest> points = pointOfInterestRepository
                .findActiveNavigableByMapIdAndType(mapId, pointType);
        List<MapNode> pointNodes = points.stream()
                .filter(PointOfInterest::isActive)
                .peek(point -> validateMapElement(point.getStoreMap(), mapId, "point of interest"))
                .map(this::navigationNodeFor)
                .filter(Objects::nonNull)
                .filter(MapNode::isActive)
                .peek(node -> validateMapElement(node.getStoreMap(), mapId, "point of interest navigation node"))
                .toList();

        if (pointNodes.size() > 1) {
            throw new RouteConfigurationException(
                    "More than one active navigable " + pointType.name().toLowerCase() + " is configured"
            );
        }
        if (pointNodes.size() == 1) {
            return pointNodes.getFirst();
        }

        List<MapNode> typedNodes = activeNodes.stream()
                .filter(node -> node.getType() == nodeType)
                .toList();
        if (typedNodes.size() > 1) {
            throw new RouteConfigurationException(
                    "More than one active " + nodeType.name().toLowerCase() + " node is configured"
            );
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

    private RouteResponse toResponse(
            UUID storeId,
            UUID mapId,
            List<ProductLocation> orderedLocations,
            RouteComposer.RouteComposition composition
    ) {
        List<RouteStopResponse> orderedStops = IntStream.range(0, orderedLocations.size())
                .mapToObj(index -> {
                    ProductLocation location = orderedLocations.get(index);
                    Product product = location.getStoreProduct().getProduct();
                    return new RouteStopResponse(
                            product.getId(),
                            product.getName(),
                            location.getId(),
                            location.getNavigationNodeId(),
                            index + 1
                    );
                })
                .toList();
        List<RoutePathNodeResponse> path = composition.path().stream()
                .map(node -> new RoutePathNodeResponse(node.getId(), node.getX(), node.getY()))
                .toList();
        return new RouteResponse(storeId, mapId, orderedStops, path, composition.distanceMeters());
    }

    private void validateMapElement(StoreMap referencedMap, UUID mapId, String elementType) {
        if (referencedMap != null && !Objects.equals(referencedMap.getId(), mapId)) {
            throw new MapConsistencyException("Map element belongs to another map: " + elementType);
        }
    }

    private UUID requireIdentifier(UUID value, String message) {
        if (value == null) {
            throw new MapConsistencyException(message);
        }
        return value;
    }

    private void inconsistentLocation(ProductLocation location, String reason) {
        throw new ProductLocationConsistencyException(location.getId(), reason);
    }
}
