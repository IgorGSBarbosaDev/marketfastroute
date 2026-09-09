package com.marketfastroute.product;

import java.util.UUID;

public class ProductLocationNotFoundException extends RuntimeException {

    public ProductLocationNotFoundException(UUID storeId, UUID productId, UUID locationId) {
        super("Product location not found: store=" + storeId + ", product=" + productId + ", location=" + locationId);
    }

    public ProductLocationNotFoundException(UUID storeId, UUID productId) {
        super("Primary product location not found: store=" + storeId + ", product=" + productId);
    }
}
