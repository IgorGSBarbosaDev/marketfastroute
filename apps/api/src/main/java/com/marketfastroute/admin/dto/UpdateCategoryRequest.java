package com.marketfastroute.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record UpdateCategoryRequest(
        @NotBlank String name,
        @NotBlank String code,
        UUID parentId,
        @NotNull Boolean active
) {
}
