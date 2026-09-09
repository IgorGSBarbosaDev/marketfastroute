package com.marketfastroute.product;

import org.springframework.stereotype.Component;

@Component
public class ProductLocationMapper {

    public ProductLocationResponse toResponse(ProductLocation location) {
        return new ProductLocationResponse(
                location.getId(),
                location.getStoreProduct().getProduct().getId(),
                location.getStoreId(),
                location.getMapId(),
                location.getSector() == null ? null : location.getSector().getName(),
                location.getAisle() == null ? null : location.getAisle().getName(),
                location.getShelfBlock() == null ? null : location.getShelfBlock().getName(),
                location.getSide(),
                location.getModule(),
                location.getShelfLevel(),
                location.getX(),
                location.getY(),
                location.getNavigationNodeId(),
                location.isPrimaryLocation()
        );
    }
}
