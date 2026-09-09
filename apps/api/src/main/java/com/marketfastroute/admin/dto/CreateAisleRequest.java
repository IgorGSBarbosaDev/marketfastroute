package com.marketfastroute.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record CreateAisleRequest(
        UUID sectorId,
        @NotBlank String code,
        @NotBlank String name,
        @NotNull BigDecimal x,
        @NotNull BigDecimal y,
        @NotNull @Positive BigDecimal width,
        @NotNull @Positive BigDecimal height,
        @NotNull BigDecimal rotation,
        Boolean active
) {
    public CreateAisleRequest {
        active = active == null ? Boolean.TRUE : active;
    }
}
