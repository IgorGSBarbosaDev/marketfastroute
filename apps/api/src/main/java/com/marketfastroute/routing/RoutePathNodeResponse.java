package com.marketfastroute.routing;

import java.math.BigDecimal;
import java.util.UUID;

public record RoutePathNodeResponse(
        UUID nodeId,
        BigDecimal x,
        BigDecimal y
) {
}
