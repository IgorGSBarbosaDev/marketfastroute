package com.marketfastroute.store;

import java.util.UUID;

public record StoreResponse(
        UUID id,
        String name,
        String code,
        String address,
        String city,
        String state,
        boolean active
) {
}
