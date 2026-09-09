package com.marketfastroute.map;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record StoreMapResponse(
        UUID id,
        UUID storeId,
        int version,
        String name,
        BigDecimal width,
        BigDecimal height,
        BigDecimal scaleMetersPerUnit,
        List<SectorResponse> sectors,
        List<AisleResponse> aisles,
        List<ShelfBlockResponse> shelfBlocks,
        List<PointOfInterestResponse> pointsOfInterest,
        List<MapNodeResponse> nodes,
        List<MapEdgeResponse> edges
) {

    public StoreMapResponse {
        sectors = List.copyOf(sectors);
        aisles = List.copyOf(aisles);
        shelfBlocks = List.copyOf(shelfBlocks);
        pointsOfInterest = List.copyOf(pointsOfInterest);
        nodes = List.copyOf(nodes);
        edges = List.copyOf(edges);
    }
}
