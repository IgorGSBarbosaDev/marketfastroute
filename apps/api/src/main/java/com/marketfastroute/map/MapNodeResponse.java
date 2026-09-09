package com.marketfastroute.map;

import java.math.BigDecimal;
import java.util.UUID;

public record MapNodeResponse(
        UUID id,
        MapNodeType type,
        BigDecimal x,
        BigDecimal y,
        String label
) {
}
