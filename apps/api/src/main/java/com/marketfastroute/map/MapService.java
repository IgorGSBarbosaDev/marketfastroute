package com.marketfastroute.map;

import com.marketfastroute.store.StoreMap;
import com.marketfastroute.store.StoreMapRepository;
import com.marketfastroute.store.StoreNotFoundException;
import com.marketfastroute.store.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Predicate;

@Service
@Transactional(readOnly = true)
public class MapService {

    private final StoreRepository storeRepository;
    private final StoreMapRepository storeMapRepository;
    private final SectorRepository sectorRepository;
    private final AisleRepository aisleRepository;
    private final ShelfBlockRepository shelfBlockRepository;
    private final PointOfInterestRepository pointOfInterestRepository;
    private final MapNodeRepository mapNodeRepository;
    private final MapEdgeRepository mapEdgeRepository;
    private final MapMapper mapMapper;

    public MapService(
            StoreRepository storeRepository,
            StoreMapRepository storeMapRepository,
            SectorRepository sectorRepository,
            AisleRepository aisleRepository,
            ShelfBlockRepository shelfBlockRepository,
            PointOfInterestRepository pointOfInterestRepository,
            MapNodeRepository mapNodeRepository,
            MapEdgeRepository mapEdgeRepository,
            MapMapper mapMapper
    ) {
        this.storeRepository = storeRepository;
        this.storeMapRepository = storeMapRepository;
        this.sectorRepository = sectorRepository;
        this.aisleRepository = aisleRepository;
        this.shelfBlockRepository = shelfBlockRepository;
        this.pointOfInterestRepository = pointOfInterestRepository;
        this.mapNodeRepository = mapNodeRepository;
        this.mapEdgeRepository = mapEdgeRepository;
        this.mapMapper = mapMapper;
    }

    public StoreMapResponse findActiveByStore(UUID storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new StoreNotFoundException(storeId);
        }

        StoreMap storeMap = storeMapRepository.findActiveByStoreId(storeId)
                .orElseThrow(() -> new StoreMapNotFoundException(storeId));

        if (storeMap.getStatus() != MapStatus.ACTIVE) {
            throw new StoreMapNotFoundException(storeId);
        }

        if (storeMap.getStore() == null || !Objects.equals(storeMap.getStore().getId(), storeId)) {
            throw new MapConsistencyException("Active map belongs to another store");
        }

        UUID mapId = storeMap.getId();
        if (mapId == null) {
            throw new MapConsistencyException("Active map has no identifier");
        }
        List<Sector> sectors = activeElements(
                sectorRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId),
                Sector::isActive,
                Sector::getStoreMap,
                mapId,
                "sector"
        );
        List<Aisle> aisles = activeElements(
                aisleRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId),
                Aisle::isActive,
                Aisle::getStoreMap,
                mapId,
                "aisle"
        );
        List<ShelfBlock> shelfBlocks = activeElements(
                shelfBlockRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId),
                ShelfBlock::isActive,
                ShelfBlock::getStoreMap,
                mapId,
                "shelf block"
        );
        List<PointOfInterest> pointsOfInterest = activeElements(
                pointOfInterestRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId),
                PointOfInterest::isActive,
                PointOfInterest::getStoreMap,
                mapId,
                "point of interest"
        );
        List<MapNode> nodes = activeElements(
                mapNodeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId),
                MapNode::isActive,
                MapNode::getStoreMap,
                mapId,
                "map node"
        );
        List<MapEdge> edges = activeElements(
                mapEdgeRepository.findByStoreMap_IdAndActiveTrueOrderById(mapId),
                MapEdge::isActive,
                MapEdge::getStoreMap,
                mapId,
                "map edge"
        );

        validatePointOfInterestNodes(pointsOfInterest, mapId);
        validateEdgeNodes(edges, mapId);

        return mapMapper.toResponse(storeMap, sectors, aisles, shelfBlocks, pointsOfInterest, nodes, edges);
    }

    private <T> List<T> activeElements(
            List<T> elements,
            Predicate<T> active,
            Function<T, StoreMap> mapReference,
            UUID mapId,
            String elementType
    ) {
        List<T> activeElements = elements.stream()
                .filter(active)
                .toList();
        activeElements.forEach(element -> validateMapReference(mapReference.apply(element), mapId, elementType));
        return activeElements;
    }

    private void validateMapReference(StoreMap referencedMap, UUID mapId, String elementType) {
        if (referencedMap == null || !Objects.equals(referencedMap.getId(), mapId)) {
            throw new MapConsistencyException("Map element belongs to another map: " + elementType);
        }
    }

    private void validatePointOfInterestNodes(List<PointOfInterest> pointsOfInterest, UUID mapId) {
        pointsOfInterest.stream()
                .filter(pointOfInterest -> pointOfInterest.getNavigationNode() != null)
                .forEach(pointOfInterest -> validateMapReference(
                        pointOfInterest.getNavigationNode().getStoreMap(),
                        mapId,
                        "point of interest navigation node"
                ));
    }

    private void validateEdgeNodes(List<MapEdge> edges, UUID mapId) {
        edges.forEach(edge -> {
            if (edge.getFromNode() != null) {
                validateMapReference(edge.getFromNode().getStoreMap(), mapId, "edge origin node");
            }
            if (edge.getToNode() != null) {
                validateMapReference(edge.getToNode().getStoreMap(), mapId, "edge destination node");
            }
        });
    }
}
