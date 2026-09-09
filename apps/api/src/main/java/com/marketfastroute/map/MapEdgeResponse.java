package com.marketfastroute.map;

import java.math.BigDecimal;
import java.util.UUID;

public record MapEdgeResponse(
        UUID id,
        UUID fromNodeId,
        UUID toNodeId,
        BigDecimal distanceMeters,
        boolean bidirectional
) {
}
