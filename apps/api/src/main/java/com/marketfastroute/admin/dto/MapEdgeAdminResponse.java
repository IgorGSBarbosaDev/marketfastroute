package com.marketfastroute.admin.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record MapEdgeAdminResponse(
        UUID id,
        UUID mapId,
        UUID fromNodeId,
        UUID toNodeId,
        BigDecimal distanceMeters,
        boolean bidirectional,
        boolean active
) {
}
