package com.marketfastroute.map;

import com.marketfastroute.admin.AdminConflictException;
import com.marketfastroute.admin.AdminResourceNotFoundException;
import com.marketfastroute.admin.AdminValidationException;
import com.marketfastroute.admin.dto.CreateMapEdgeRequest;
import com.marketfastroute.admin.dto.CreateMapNodeRequest;
import com.marketfastroute.admin.dto.CreatePointOfInterestRequest;
import com.marketfastroute.admin.dto.MapEdgeAdminResponse;
import com.marketfastroute.admin.dto.MapNodeAdminResponse;
import com.marketfastroute.admin.dto.PointOfInterestAdminResponse;
import com.marketfastroute.admin.dto.UpdateMapEdgeRequest;
import com.marketfastroute.admin.dto.UpdateMapNodeRequest;
import com.marketfastroute.admin.dto.UpdatePointOfInterestRequest;
import com.marketfastroute.store.StoreMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MapGraphAdminService {

    private final MapAdminSupport support;
    private final PointOfInterestRepository pointOfInterestRepository;
    private final MapNodeRepository mapNodeRepository;
    private final MapEdgeRepository mapEdgeRepository;

    public MapGraphAdminService(
            MapAdminSupport support,
            PointOfInterestRepository pointOfInterestRepository,
            MapNodeRepository mapNodeRepository,
            MapEdgeRepository mapEdgeRepository
    ) {
        this.support = support;
        this.pointOfInterestRepository = pointOfInterestRepository;
        this.mapNodeRepository = mapNodeRepository;
        this.mapEdgeRepository = mapEdgeRepository;
    }

    public List<PointOfInterestAdminResponse> findPointsOfInterest(UUID mapId) {
        support.findMap(mapId);
        return pointOfInterestRepository.findByStoreMap_IdOrderById(mapId).stream()
                .map(this::toResponse)
                .toList();
    }

    public PointOfInterestAdminResponse findPointOfInterest(UUID mapId, UUID pointOfInterestId) {
        return toResponse(pointOfInterestRepository.findByStoreMap_IdAndId(mapId, pointOfInterestId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Point of interest")));
    }

    @Transactional
    public PointOfInterestAdminResponse createPointOfInterest(UUID mapId, CreatePointOfInterestRequest request) {
        StoreMap map = support.findMap(mapId);
        MapNode node = resolveNode(mapId, request.navigationNodeId());

        PointOfInterest pointOfInterest = new PointOfInterest();
        pointOfInterest.setStoreMap(map);
        apply(pointOfInterest, request, node);
        return toResponse(pointOfInterestRepository.save(pointOfInterest));
    }

    @Transactional
    public PointOfInterestAdminResponse updatePointOfInterest(
            UUID mapId,
            UUID pointOfInterestId,
            UpdatePointOfInterestRequest request
    ) {
        PointOfInterest pointOfInterest = pointOfInterestRepository.findByStoreMap_IdAndId(mapId, pointOfInterestId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Point of interest"));
        MapNode node = resolveNode(mapId, request.navigationNodeId());
        pointOfInterest.setNavigationNodeId(node == null ? null : node.getId());
        pointOfInterest.setType(request.type());
        pointOfInterest.setName(request.name());
        pointOfInterest.setX(request.x());
        pointOfInterest.setY(request.y());
        pointOfInterest.setActive(request.active());
        return toResponse(pointOfInterestRepository.save(pointOfInterest));
    }

    public List<MapNodeAdminResponse> findNodes(UUID mapId) {
        support.findMap(mapId);
        return mapNodeRepository.findByStoreMap_IdOrderById(mapId).stream().map(this::toResponse).toList();
    }

    public MapNodeAdminResponse findNode(UUID mapId, UUID nodeId) {
        return toResponse(resolveNode(mapId, nodeId));
    }

    @Transactional
    public MapNodeAdminResponse createNode(UUID mapId, CreateMapNodeRequest request) {
        StoreMap map = support.findMap(mapId);
        MapNode node = new MapNode();
        node.setStoreMap(map);
        node.setType(request.type());
        node.setX(request.x());
        node.setY(request.y());
        node.setLabel(request.label());
        node.setActive(request.active());
        return toResponse(mapNodeRepository.save(node));
    }

    @Transactional
    public MapNodeAdminResponse updateNode(UUID mapId, UUID nodeId, UpdateMapNodeRequest request) {
        MapNode node = resolveNode(mapId, nodeId);
        node.setType(request.type());
        node.setX(request.x());
        node.setY(request.y());
        node.setLabel(request.label());
        node.setActive(request.active());
        return toResponse(mapNodeRepository.save(node));
    }

    public List<MapEdgeAdminResponse> findEdges(UUID mapId) {
        support.findMap(mapId);
        return mapEdgeRepository.findByStoreMap_IdOrderById(mapId).stream().map(this::toResponse).toList();
    }

    public MapEdgeAdminResponse findEdge(UUID mapId, UUID edgeId) {
        return toResponse(mapEdgeRepository.findByStoreMap_IdAndId(mapId, edgeId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Map edge")));
    }

    @Transactional
    public MapEdgeAdminResponse createEdge(UUID mapId, CreateMapEdgeRequest request) {
        StoreMap map = support.findMap(mapId);
        MapNode fromNode = resolveNode(mapId, request.fromNodeId());
        MapNode toNode = resolveNode(mapId, request.toNodeId());
        ensureDistinctNodes(fromNode, toNode);
        ensureEdgeIsUnique(mapId, request.fromNodeId(), request.toNodeId(), null);

        MapEdge edge = new MapEdge();
        edge.setStoreMap(map);
        edge.setFromNodeId(fromNode.getId());
        edge.setToNodeId(toNode.getId());
        edge.setDistanceMeters(request.distanceMeters());
        edge.setBidirectional(request.bidirectional());
        edge.setActive(request.active());
        return toResponse(mapEdgeRepository.save(edge));
    }

    @Transactional
    public MapEdgeAdminResponse updateEdge(UUID mapId, UUID edgeId, UpdateMapEdgeRequest request) {
        MapEdge edge = mapEdgeRepository.findByStoreMap_IdAndId(mapId, edgeId)
                .orElseThrow(() -> new com.marketfastroute.admin.AdminResourceNotFoundException("Map edge"));
        MapNode fromNode = resolveNode(mapId, request.fromNodeId());
        MapNode toNode = resolveNode(mapId, request.toNodeId());
        ensureDistinctNodes(fromNode, toNode);
        ensureEdgeIsUnique(mapId, request.fromNodeId(), request.toNodeId(), edgeId);

        edge.setFromNodeId(fromNode.getId());
        edge.setToNodeId(toNode.getId());
        edge.setDistanceMeters(request.distanceMeters());
        edge.setBidirectional(request.bidirectional());
        edge.setActive(request.active());
        return toResponse(mapEdgeRepository.save(edge));
    }

    private MapNode resolveNode(UUID mapId, UUID nodeId) {
        if (nodeId == null) {
            return null;
        }
        return mapNodeRepository.findByStoreMap_IdAndId(mapId, nodeId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Map node"));
    }

    private void ensureDistinctNodes(MapNode fromNode, MapNode toNode) {
        if (Objects.equals(fromNode.getId(), toNode.getId())) {
            throw new AdminValidationException("Map edge cannot connect a node to itself");
        }
    }

    private void ensureEdgeIsUnique(UUID mapId, UUID fromNodeId, UUID toNodeId, UUID edgeId) {
        boolean exists = edgeId == null
                ? mapEdgeRepository.existsByStoreMap_IdAndFromNodeIdAndToNodeId(mapId, fromNodeId, toNodeId)
                : mapEdgeRepository.existsByStoreMap_IdAndFromNodeIdAndToNodeIdAndIdNot(
                mapId, fromNodeId, toNodeId, edgeId);
        if (exists) {
            throw new AdminConflictException("Map edge already exists");
        }
    }

    private void apply(PointOfInterest pointOfInterest, CreatePointOfInterestRequest request, MapNode node) {
        pointOfInterest.setNavigationNodeId(node == null ? null : node.getId());
        pointOfInterest.setType(request.type());
        pointOfInterest.setName(request.name());
        pointOfInterest.setX(request.x());
        pointOfInterest.setY(request.y());
        pointOfInterest.setActive(request.active());
    }

    private PointOfInterestAdminResponse toResponse(PointOfInterest pointOfInterest) {
        return new PointOfInterestAdminResponse(
                pointOfInterest.getId(), pointOfInterest.getStoreMap().getId(), pointOfInterest.getNavigationNodeId(),
                pointOfInterest.getType(), pointOfInterest.getName(), pointOfInterest.getX(), pointOfInterest.getY(),
                pointOfInterest.isActive());
    }

    private MapNodeAdminResponse toResponse(MapNode node) {
        return new MapNodeAdminResponse(
                node.getId(), node.getStoreMap().getId(), node.getType(), node.getX(), node.getY(),
                node.getLabel(), node.isActive());
    }

    private MapEdgeAdminResponse toResponse(MapEdge edge) {
        return new MapEdgeAdminResponse(
                edge.getId(), edge.getStoreMap().getId(), edge.getFromNodeId(), edge.getToNodeId(),
                edge.getDistanceMeters(), edge.isBidirectional(), edge.isActive());
    }
}
