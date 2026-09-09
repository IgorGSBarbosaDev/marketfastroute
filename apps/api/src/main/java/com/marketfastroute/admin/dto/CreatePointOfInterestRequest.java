package com.marketfastroute.admin.dto;

import com.marketfastroute.map.PointOfInterestType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record CreatePointOfInterestRequest(
        UUID navigationNodeId,
        @NotNull PointOfInterestType type,
        @NotBlank String name,
        @NotNull BigDecimal x,
        @NotNull BigDecimal y,
        Boolean active
) {
    public CreatePointOfInterestRequest {
        active = active == null ? Boolean.TRUE : active;
    }
}
