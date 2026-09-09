package com.marketfastroute.routing;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RouteResponse(
        UUID storeId,
        UUID mapId,
        List<RouteStopResponse> orderedStops,
        List<RoutePathNodeResponse> path,
        BigDecimal distanceMeters
) {

    public RouteResponse {
        orderedStops = List.copyOf(orderedStops);
        path = List.copyOf(path);
    }
}
