package com.marketfastroute.admin.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateMapEdgeRequest(
        @NotNull UUID fromNodeId,
        @NotNull UUID toNodeId,
        @NotNull @Positive BigDecimal distanceMeters,
        @NotNull Boolean bidirectional,
        Boolean active
) {
    public CreateMapEdgeRequest {
        active = active == null ? Boolean.TRUE : active;
    }
}
