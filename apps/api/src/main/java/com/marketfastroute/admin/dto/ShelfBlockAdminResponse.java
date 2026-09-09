package com.marketfastroute.admin.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record ShelfBlockAdminResponse(
        UUID id,
        UUID mapId,
        UUID sectorId,
        UUID aisleId,
        String code,
        String name,
        BigDecimal x,
        BigDecimal y,
        BigDecimal width,
        BigDecimal height,
        BigDecimal rotation,
        boolean active
) {
}
