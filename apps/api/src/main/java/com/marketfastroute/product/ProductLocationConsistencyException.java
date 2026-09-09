package com.marketfastroute.product;

import java.util.UUID;

public class ProductLocationConsistencyException extends RuntimeException {

    public ProductLocationConsistencyException(UUID locationId, String reason) {
        super("Product location " + locationId + " is inconsistent: " + reason);
    }
}
