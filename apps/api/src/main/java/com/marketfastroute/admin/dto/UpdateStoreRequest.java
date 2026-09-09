package com.marketfastroute.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateStoreRequest(
        @NotBlank String name,
        @NotBlank String code,
        @NotBlank String address,
        @NotBlank String city,
        @NotBlank String state,
        @NotNull Boolean active
) {
}
