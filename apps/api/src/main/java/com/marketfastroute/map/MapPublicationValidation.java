package com.marketfastroute.map;

import java.util.List;
import java.util.UUID;

public record MapPublicationValidation(
        UUID mapId,
        boolean publishable,
        List<MapPublicationIssue> issues
) {
    public MapPublicationValidation {
        issues = List.copyOf(issues);
    }
}
