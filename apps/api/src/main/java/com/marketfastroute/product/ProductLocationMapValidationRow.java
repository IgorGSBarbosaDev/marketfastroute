package com.marketfastroute.product;

import java.math.BigDecimal;
import java.util.UUID;

public record ProductLocationMapValidationRow(
        UUID id,
        UUID productId,
        UUID navigationNodeId,
        BigDecimal x,
        BigDecimal y,
        boolean primaryLocation
) {
}
