package com.marketfastroute.admin.dto;

import com.marketfastroute.map.MapStatus;

import java.math.BigDecimal;
import java.util.UUID;

public record StoreMapAdminResponse(
        UUID id,
        UUID storeId,
        int version,
        String name,
        BigDecimal width,
        BigDecimal height,
        BigDecimal scaleMetersPerUnit,
        MapStatus status
) {
}
