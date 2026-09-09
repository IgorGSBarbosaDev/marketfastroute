package com.marketfastroute.admin.dto;

import java.util.UUID;

public record StoreProductAdminResponse(
        UUID id,
        UUID storeId,
        UUID productId,
        boolean active
) {
}
