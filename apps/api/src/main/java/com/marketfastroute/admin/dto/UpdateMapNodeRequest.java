package com.marketfastroute.admin.dto;

import com.marketfastroute.map.MapNodeType;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record UpdateMapNodeRequest(
        @NotNull MapNodeType type,
        @NotNull BigDecimal x,
        @NotNull BigDecimal y,
        String label,
        @NotNull Boolean active
) {
}
