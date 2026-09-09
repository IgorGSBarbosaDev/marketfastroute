package com.marketfastroute.map;

import java.math.BigDecimal;
import java.util.UUID;

public record ShelfBlockResponse(
        UUID id,
        UUID sectorId,
        UUID aisleId,
        String code,
        String name,
        BigDecimal x,
        BigDecimal y,
        BigDecimal width,
        BigDecimal height,
        BigDecimal rotation
) {
}
