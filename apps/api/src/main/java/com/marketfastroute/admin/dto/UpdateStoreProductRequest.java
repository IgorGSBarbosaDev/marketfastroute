package com.marketfastroute.admin.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateStoreProductRequest(@NotNull Boolean active) {
}
