package com.marketfastroute.admin.dto;

import com.marketfastroute.product.ProductLocationSide;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductLocationAdminResponse(
        UUID id,
        UUID storeId,
        UUID storeProductId,
        UUID mapId,
        UUID sectorId,
        UUID aisleId,
        UUID shelfBlockId,
        ProductLocationSide side,
        String module,
        Integer shelfLevel,
        BigDecimal x,
        BigDecimal y,
        UUID navigationNodeId,
        boolean primaryLocation,
        boolean active
) {
}
