package com.marketfastroute.admin.dto;

import java.util.UUID;

public record ProductAdminResponse(
        UUID id,
        UUID categoryId,
        String sku,
        String ean,
        String name,
        String brand,
        String description,
        boolean active
) {
}
