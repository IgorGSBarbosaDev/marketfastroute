package com.marketfastroute.product;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/stores/{storeId}/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<ProductResponse> findAvailable(
            @PathVariable("storeId") UUID storeId,
            @RequestParam(value = "search", required = false) String search
    ) {
        return productService.findAvailableByStore(storeId, search);
    }

    @GetMapping("/{productId}")
    public ProductResponse findById(
            @PathVariable("storeId") UUID storeId,
            @PathVariable("productId") UUID productId
    ) {
        return productService.findById(storeId, productId);
    }
}
