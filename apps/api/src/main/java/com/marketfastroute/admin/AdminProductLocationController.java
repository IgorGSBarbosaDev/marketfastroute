package com.marketfastroute.admin;

import com.marketfastroute.admin.dto.CreateProductLocationRequest;
import com.marketfastroute.admin.dto.ProductLocationAdminResponse;
import com.marketfastroute.admin.dto.UpdateProductLocationRequest;
import com.marketfastroute.product.ProductLocationAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/stores/{storeId}/product-locations")
public class AdminProductLocationController {

    private final ProductLocationAdminService productLocationAdminService;

    public AdminProductLocationController(ProductLocationAdminService productLocationAdminService) {
        this.productLocationAdminService = productLocationAdminService;
    }

    @GetMapping
    public List<ProductLocationAdminResponse> findByStore(@PathVariable UUID storeId) {
        return productLocationAdminService.findByStore(storeId);
    }

    @GetMapping("/{locationId}")
    public ProductLocationAdminResponse findById(
            @PathVariable UUID storeId,
            @PathVariable UUID locationId
    ) {
        return productLocationAdminService.findById(storeId, locationId);
    }

    @PostMapping
    public ResponseEntity<ProductLocationAdminResponse> create(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateProductLocationRequest request
    ) {
        ProductLocationAdminResponse response = productLocationAdminService.create(storeId, request);
        return ResponseEntity.created(URI.create(
                "/api/v1/admin/stores/" + storeId + "/product-locations/" + response.id())).body(response);
    }

    @PutMapping("/{locationId}")
    public ProductLocationAdminResponse update(
            @PathVariable UUID storeId,
            @PathVariable UUID locationId,
            @Valid @RequestBody UpdateProductLocationRequest request
    ) {
        return productLocationAdminService.update(storeId, locationId, request);
    }
}
