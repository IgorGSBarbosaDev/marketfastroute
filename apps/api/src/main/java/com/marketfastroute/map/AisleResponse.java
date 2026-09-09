package com.marketfastroute.map;

import java.math.BigDecimal;
import java.util.UUID;

public record AisleResponse(
        UUID id,
        UUID sectorId,
        String code,
        String name,
        BigDecimal x,
        BigDecimal y,
        BigDecimal width,
        BigDecimal height,
        BigDecimal rotation
) {
}
