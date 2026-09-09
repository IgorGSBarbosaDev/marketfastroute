package com.marketfastroute.product;

import org.springframework.stereotype.Component;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getSku(),
                product.getEan(),
                product.getBrand(),
                product.getCategory().getName()
        );
    }
}
