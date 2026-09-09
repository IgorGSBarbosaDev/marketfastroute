package com.marketfastroute.admin;

import com.marketfastroute.admin.dto.CategoryAdminResponse;
import com.marketfastroute.admin.dto.CreateCategoryRequest;
import com.marketfastroute.admin.dto.CreateProductRequest;
import com.marketfastroute.admin.dto.CreateStoreProductRequest;
import com.marketfastroute.admin.dto.ProductAdminResponse;
import com.marketfastroute.admin.dto.StoreProductAdminResponse;
import com.marketfastroute.admin.dto.UpdateCategoryRequest;
import com.marketfastroute.admin.dto.UpdateProductRequest;
import com.marketfastroute.admin.dto.UpdateStoreProductRequest;
import com.marketfastroute.product.CatalogAdminService;
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
@RequestMapping("/api/v1/admin")
public class AdminCatalogController {

    private final CatalogAdminService catalogAdminService;

    public AdminCatalogController(CatalogAdminService catalogAdminService) {
        this.catalogAdminService = catalogAdminService;
    }

    @GetMapping("/categories")
    public List<CategoryAdminResponse> findCategories() {
        return catalogAdminService.findCategories();
    }

    @GetMapping("/categories/{categoryId}")
    public CategoryAdminResponse findCategory(@PathVariable UUID categoryId) {
        return catalogAdminService.findCategory(categoryId);
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryAdminResponse> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryAdminResponse response = catalogAdminService.createCategory(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/categories/" + response.id())).body(response);
    }

    @PutMapping("/categories/{categoryId}")
    public CategoryAdminResponse updateCategory(
            @PathVariable UUID categoryId,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        return catalogAdminService.updateCategory(categoryId, request);
    }

    @GetMapping("/products")
    public List<ProductAdminResponse> findProducts() {
        return catalogAdminService.findProducts();
    }

    @GetMapping("/products/{productId}")
    public ProductAdminResponse findProduct(@PathVariable UUID productId) {
        return catalogAdminService.findProduct(productId);
    }

    @PostMapping("/products")
    public ResponseEntity<ProductAdminResponse> createProduct(@Valid @RequestBody CreateProductRequest request) {
        ProductAdminResponse response = catalogAdminService.createProduct(request);
        return ResponseEntity.created(URI.create("/api/v1/admin/products/" + response.id())).body(response);
    }

    @PutMapping("/products/{productId}")
    public ProductAdminResponse updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        return catalogAdminService.updateProduct(productId, request);
    }

    @GetMapping("/stores/{storeId}/products")
    public List<StoreProductAdminResponse> findStoreProducts(@PathVariable UUID storeId) {
        return catalogAdminService.findStoreProducts(storeId);
    }

    @GetMapping("/stores/{storeId}/products/{storeProductId}")
    public StoreProductAdminResponse findStoreProduct(
            @PathVariable UUID storeId,
            @PathVariable UUID storeProductId
    ) {
        return catalogAdminService.findStoreProduct(storeId, storeProductId);
    }

    @PostMapping("/stores/{storeId}/products")
    public ResponseEntity<StoreProductAdminResponse> createStoreProduct(
            @PathVariable UUID storeId,
            @Valid @RequestBody CreateStoreProductRequest request
    ) {
        StoreProductAdminResponse response = catalogAdminService.createStoreProduct(storeId, request);
        return ResponseEntity.created(URI.create(
                "/api/v1/admin/stores/" + storeId + "/products/" + response.id())).body(response);
    }

    @PutMapping("/stores/{storeId}/products/{storeProductId}")
    public StoreProductAdminResponse updateStoreProduct(
            @PathVariable UUID storeId,
            @PathVariable UUID storeProductId,
            @Valid @RequestBody UpdateStoreProductRequest request
    ) {
        return catalogAdminService.updateStoreProduct(storeId, storeProductId, request);
    }
}
