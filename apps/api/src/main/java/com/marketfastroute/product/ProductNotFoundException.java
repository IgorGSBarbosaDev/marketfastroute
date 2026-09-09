package com.marketfastroute.product;

import java.util.UUID;

public class ProductNotFoundException extends RuntimeException {

    public ProductNotFoundException(UUID storeId, UUID productId) {
        super("Product not found in store: " + productId + " / " + storeId);
    }
}
