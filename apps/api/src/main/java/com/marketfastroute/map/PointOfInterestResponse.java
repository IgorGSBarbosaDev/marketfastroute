package com.marketfastroute.map;

import java.math.BigDecimal;
import java.util.UUID;

public record PointOfInterestResponse(
        UUID id,
        PointOfInterestType type,
        String name,
        BigDecimal x,
        BigDecimal y,
        UUID navigationNodeId
) {
}
