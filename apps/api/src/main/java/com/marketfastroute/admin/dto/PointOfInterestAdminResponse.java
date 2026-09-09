package com.marketfastroute.admin.dto;

import com.marketfastroute.map.PointOfInterestType;

import java.math.BigDecimal;
import java.util.UUID;

public record PointOfInterestAdminResponse(
        UUID id,
        UUID mapId,
        UUID navigationNodeId,
        PointOfInterestType type,
        String name,
        BigDecimal x,
        BigDecimal y,
        boolean active
) {
}
