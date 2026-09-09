package com.marketfastroute.routing;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record RouteRequest(
        @NotNull UUID storeId,
        @NotEmpty List<@NotNull UUID> productIds
) {
}
