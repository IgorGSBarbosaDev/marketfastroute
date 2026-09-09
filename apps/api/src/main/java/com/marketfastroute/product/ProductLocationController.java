package com.marketfastroute.product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stores/{storeId}/products/{productId}/locations")
public class ProductLocationController {

    private final ProductLocationService productLocationService;

    public ProductLocationController(ProductLocationService productLocationService) {
        this.productLocationService = productLocationService;
    }

    @GetMapping
    public List<ProductLocationResponse> findByProduct(
            @PathVariable UUID storeId,
            @PathVariable UUID productId
    ) {
        return productLocationService.findByProduct(storeId, productId);
    }

    @GetMapping("/primary")
    public ProductLocationResponse findPrimaryByProduct(
            @PathVariable UUID storeId,
            @PathVariable UUID productId
    ) {
        return productLocationService.findPrimaryByProduct(storeId, productId);
    }

    @GetMapping("/{locationId}")
    public ProductLocationResponse findById(
            @PathVariable UUID storeId,
            @PathVariable UUID productId,
            @PathVariable UUID locationId
    ) {
        return productLocationService.findById(storeId, productId, locationId);
    }
}
