package com.marketfastroute.admin.dto;

import com.marketfastroute.map.MapNodeType;

import java.math.BigDecimal;
import java.util.UUID;

public record MapNodeAdminResponse(
        UUID id,
        UUID mapId,
        MapNodeType type,
        BigDecimal x,
        BigDecimal y,
        String label,
        boolean active
) {
}
