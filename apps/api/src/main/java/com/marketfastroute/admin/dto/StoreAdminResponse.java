package com.marketfastroute.admin.dto;

import java.util.UUID;

public record StoreAdminResponse(
        UUID id,
        String name,
        String code,
        String address,
        String city,
        String state,
        boolean active
) {
}
