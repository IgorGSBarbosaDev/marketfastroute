package com.marketfastroute.product;

import java.util.UUID;

public record ProductResponse(
        UUID id,
        String name,
        String sku,
        String ean,
        String brand,
        String category
) {
}
