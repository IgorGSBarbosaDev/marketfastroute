package com.marketfastroute.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateShelfBlockRequest(
        UUID sectorId,
        UUID aisleId,
        @NotBlank String code,
        String name,
        @NotNull BigDecimal x,
        @NotNull BigDecimal y,
        @NotNull @Positive BigDecimal width,
        @NotNull @Positive BigDecimal height,
        @NotNull BigDecimal rotation,
        @NotNull Boolean active
) {
}
