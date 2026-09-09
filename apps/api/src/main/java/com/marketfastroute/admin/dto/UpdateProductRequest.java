package com.marketfastroute.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateProductRequest(
        @NotNull UUID categoryId,
        @NotBlank String sku,
        String ean,
        @NotBlank String name,
        String brand,
        String description,
        @NotNull Boolean active
) {
}
