package com.marketfastroute.map;

import java.math.BigDecimal;
import java.util.UUID;

public record SectorResponse(
        UUID id,
        String name,
        String code,
        BigDecimal x,
        BigDecimal y,
        BigDecimal width,
        BigDecimal height,
        BigDecimal rotation
) {
}
