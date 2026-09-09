package com.marketfastroute.admin.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record CreateCategoryRequest(
        @NotBlank String name,
        @NotBlank String code,
        UUID parentId
) {
}
