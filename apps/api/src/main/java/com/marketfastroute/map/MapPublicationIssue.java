package com.marketfastroute.map;

import java.util.UUID;

public record MapPublicationIssue(
        String code,
        String message,
        String elementType,
        UUID elementId
) {
}
