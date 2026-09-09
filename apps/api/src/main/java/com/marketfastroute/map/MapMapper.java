package com.marketfastroute.map;

import com.marketfastroute.store.StoreMap;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class MapMapper {

    public StoreMapResponse toResponse(
            StoreMap storeMap,
            List<Sector> sectors,
            List<Aisle> aisles,
            List<ShelfBlock> shelfBlocks,
            List<PointOfInterest> pointsOfInterest,
            List<MapNode> nodes,
            List<MapEdge> edges
    ) {
        return new StoreMapResponse(
                storeMap.getId(),
                storeMap.getStore().getId(),
                storeMap.getVersion(),
                storeMap.getName(),
                storeMap.getWidth(),
                storeMap.getHeight(),
                storeMap.getScaleMetersPerUnit(),
                sectors.stream().map(this::toResponse).toList(),
                aisles.stream().map(this::toResponse).toList(),
                shelfBlocks.stream().map(this::toResponse).toList(),
                pointsOfInterest.stream().map(this::toResponse).toList(),
                nodes.stream().map(this::toResponse).toList(),
                edges.stream().map(this::toResponse).toList()
        );
    }

    private SectorResponse toResponse(Sector sector) {
        return new SectorResponse(
                sector.getId(),
                sector.getName(),
                sector.getCode(),
                sector.getX(),
                sector.getY(),
                sector.getWidth(),
                sector.getHeight(),
                sector.getRotation()
        );
    }

    private AisleResponse toResponse(Aisle aisle) {
        return new AisleResponse(
                aisle.getId(),
                aisle.getSectorId(),
                aisle.getCode(),
                aisle.getName(),
                aisle.getX(),
                aisle.getY(),
                aisle.getWidth(),
                aisle.getHeight(),
                aisle.getRotation()
        );
    }

    private ShelfBlockResponse toResponse(ShelfBlock shelfBlock) {
        return new ShelfBlockResponse(
                shelfBlock.getId(),
                shelfBlock.getSectorId(),
                shelfBlock.getAisleId(),
                shelfBlock.getCode(),
                shelfBlock.getName(),
                shelfBlock.getX(),
                shelfBlock.getY(),
                shelfBlock.getWidth(),
                shelfBlock.getHeight(),
                shelfBlock.getRotation()
        );
    }

    private PointOfInterestResponse toResponse(PointOfInterest pointOfInterest) {
        return new PointOfInterestResponse(
                pointOfInterest.getId(),
                pointOfInterest.getType(),
                pointOfInterest.getName(),
                pointOfInterest.getX(),
                pointOfInterest.getY(),
                pointOfInterest.getNavigationNodeId()
        );
    }

    private MapNodeResponse toResponse(MapNode node) {
        return new MapNodeResponse(
                node.getId(),
                node.getType(),
                node.getX(),
                node.getY(),
                node.getLabel()
        );
    }

    private MapEdgeResponse toResponse(MapEdge edge) {
        return new MapEdgeResponse(
                edge.getId(),
                edge.getFromNodeId(),
                edge.getToNodeId(),
                edge.getDistanceMeters(),
                edge.isBidirectional()
        );
    }
}
