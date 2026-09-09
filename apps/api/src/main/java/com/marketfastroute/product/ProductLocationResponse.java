package com.marketfastroute.product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductLocationResponse(
        UUID id,
        UUID productId,
        UUID storeId,
        UUID mapId,
        String sector,
        String aisle,
        String shelfBlock,
        ProductLocationSide side,
        String module,
        Integer shelfLevel,
        BigDecimal x,
        BigDecimal y,
        UUID navigationNodeId,
        boolean primaryLocation
) {
}
