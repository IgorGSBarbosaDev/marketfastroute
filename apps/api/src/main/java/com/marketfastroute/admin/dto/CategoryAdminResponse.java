package com.marketfastroute.admin.dto;

import java.util.UUID;

public record CategoryAdminResponse(
        UUID id,
        UUID parentId,
        String name,
        String code,
        boolean active
) {
}
