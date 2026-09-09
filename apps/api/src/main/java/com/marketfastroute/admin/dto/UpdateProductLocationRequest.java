package com.marketfastroute.admin.dto;

import com.marketfastroute.product.ProductLocationSide;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateProductLocationRequest(
        @NotNull UUID storeProductId,
        @NotNull UUID mapId,
        UUID sectorId,
        UUID aisleId,
        UUID shelfBlockId,
        ProductLocationSide side,
        String module,
        Integer shelfLevel,
        BigDecimal x,
        BigDecimal y,
        @NotNull UUID navigationNodeId,
        @NotNull Boolean primaryLocation,
        @NotNull Boolean active
) {
}
