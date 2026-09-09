package com.marketfastroute.admin.dto;

import com.marketfastroute.product.ProductLocationSide;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateProductLocationRequest(
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
        Boolean primaryLocation,
        Boolean active
) {
    public CreateProductLocationRequest {
        primaryLocation = primaryLocation == null ? Boolean.FALSE : primaryLocation;
        active = active == null ? Boolean.TRUE : active;
    }
}
