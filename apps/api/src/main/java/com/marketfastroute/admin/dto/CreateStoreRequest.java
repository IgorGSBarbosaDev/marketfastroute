package com.marketfastroute.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateStoreRequest(
        @NotBlank String name,
        @NotBlank String code,
        @NotBlank String address,
        @NotBlank String city,
        @NotBlank String state
) {
}
