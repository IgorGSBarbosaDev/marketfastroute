package com.marketfastroute.routing;

import java.util.UUID;

public record RouteStopResponse(
        UUID productId,
        String productName,
        UUID productLocationId,
        UUID navigationNodeId,
        int order
) {
}
