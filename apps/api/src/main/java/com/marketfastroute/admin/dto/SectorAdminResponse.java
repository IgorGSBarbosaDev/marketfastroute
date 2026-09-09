package com.marketfastroute.admin.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SectorAdminResponse(
        UUID id,
        UUID mapId,
        String name,
        String code,
        BigDecimal x,
        BigDecimal y,
        BigDecimal width,
        BigDecimal height,
        BigDecimal rotation,
        boolean active
) {
}
