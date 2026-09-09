package com.marketfastroute.map;

import java.util.UUID;

public class StoreMapNotFoundException extends RuntimeException {

    public StoreMapNotFoundException(UUID storeId) {
        super("Active map not found for store: " + storeId);
    }
}
