package com.marketfastroute.admin.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record CreateStoreProductRequest(
        @NotNull UUID productId,
        Boolean active
) {
    public CreateStoreProductRequest {
        active = active == null ? Boolean.TRUE : active;
    }
}
