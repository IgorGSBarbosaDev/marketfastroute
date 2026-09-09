package com.marketfastroute.admin.dto;

import com.marketfastroute.map.MapStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record UpdateStoreMapRequest(
        @NotNull @Positive Integer version,
        @NotBlank String name,
        @NotNull @Positive BigDecimal width,
        @NotNull @Positive BigDecimal height,
        @NotNull @Positive BigDecimal scaleMetersPerUnit,
        @NotNull MapStatus status
) {
}
