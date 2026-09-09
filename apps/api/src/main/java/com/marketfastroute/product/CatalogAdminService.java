package com.marketfastroute.product;

import com.marketfastroute.admin.AdminConflictException;
import com.marketfastroute.admin.AdminResourceNotFoundException;
import com.marketfastroute.admin.AdminValidationException;
import com.marketfastroute.admin.dto.CategoryAdminResponse;
import com.marketfastroute.admin.dto.CreateCategoryRequest;
import com.marketfastroute.admin.dto.CreateProductRequest;
import com.marketfastroute.admin.dto.CreateStoreProductRequest;
import com.marketfastroute.admin.dto.ProductAdminResponse;
import com.marketfastroute.admin.dto.StoreProductAdminResponse;
import com.marketfastroute.admin.dto.UpdateCategoryRequest;
import com.marketfastroute.admin.dto.UpdateProductRequest;
import com.marketfastroute.admin.dto.UpdateStoreProductRequest;
import com.marketfastroute.store.Store;
import com.marketfastroute.store.StoreNotFoundException;
import com.marketfastroute.store.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class CatalogAdminService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final StoreRepository storeRepository;
    private final StoreProductRepository storeProductRepository;

    public CatalogAdminService(
            CategoryRepository categoryRepository,
            ProductRepository productRepository,
            StoreRepository storeRepository,
            StoreProductRepository storeProductRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
        this.storeRepository = storeRepository;
        this.storeProductRepository = storeProductRepository;
    }

    public List<CategoryAdminResponse> findCategories() {
        return categoryRepository.findAll().stream().map(this::toResponse).toList();
    }

    public CategoryAdminResponse findCategory(UUID categoryId) {
        return toResponse(findCategoryEntity(categoryId));
    }

    @Transactional
    public CategoryAdminResponse createCategory(CreateCategoryRequest request) {
        ensureCategoryCodeIsAvailable(request.code(), null);
        Category category = new Category();
        category.setName(request.name());
        category.setCode(request.code());
        category.setParent(resolveParent(request.parentId()));
        return toResponse(categoryRepository.save(category));
    }

    @Transactional
    public CategoryAdminResponse updateCategory(UUID categoryId, UpdateCategoryRequest request) {
        Category category = findCategoryEntity(categoryId);
        ensureCategoryCodeIsAvailable(request.code(), categoryId);
        Category parent = resolveParent(request.parentId());
        ensureNoCategoryCycle(categoryId, parent);

        category.setName(request.name());
        category.setCode(request.code());
        category.setParent(parent);
        category.setActive(request.active());
        return toResponse(categoryRepository.save(category));
    }

    public List<ProductAdminResponse> findProducts() {
        return productRepository.findAll().stream().map(this::toResponse).toList();
    }

    public ProductAdminResponse findProduct(UUID productId) {
        return toResponse(findProductEntity(productId));
    }

    @Transactional
    public ProductAdminResponse createProduct(CreateProductRequest request) {
        ensureProductIdentifiersAreAvailable(request.sku(), request.ean(), null);
        Product product = new Product();
        product.setCategory(resolveCategory(request.categoryId()));
        product.setSku(request.sku());
        product.setEan(request.ean());
        product.setName(request.name());
        product.setBrand(request.brand());
        product.setDescription(request.description());
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductAdminResponse updateProduct(UUID productId, UpdateProductRequest request) {
        Product product = findProductEntity(productId);
        ensureProductIdentifiersAreAvailable(request.sku(), request.ean(), productId);

        product.setCategory(resolveCategory(request.categoryId()));
        product.setSku(request.sku());
        product.setEan(request.ean());
        product.setName(request.name());
        product.setBrand(request.brand());
        product.setDescription(request.description());
        product.setActive(request.active());
        return toResponse(productRepository.save(product));
    }

    public List<StoreProductAdminResponse> findStoreProducts(UUID storeId) {
        ensureStore(storeId);
        return storeProductRepository.findByStore_IdOrderById(storeId).stream()
                .map(this::toResponse)
                .toList();
    }

    public StoreProductAdminResponse findStoreProduct(UUID storeId, UUID storeProductId) {
        return toResponse(findStoreProductEntity(storeId, storeProductId));
    }

    @Transactional
    public StoreProductAdminResponse createStoreProduct(UUID storeId, CreateStoreProductRequest request) {
        Store store = ensureStore(storeId);
        Product product = findProductEntity(request.productId());
        if (storeProductRepository.existsByStore_IdAndProduct_Id(storeId, request.productId())) {
            throw new AdminConflictException("Product is already associated with the store");
        }

        StoreProduct storeProduct = new StoreProduct();
        storeProduct.setStore(store);
        storeProduct.setProduct(product);
        storeProduct.setActive(request.active());
        return toResponse(storeProductRepository.save(storeProduct));
    }

    @Transactional
    public StoreProductAdminResponse updateStoreProduct(
            UUID storeId,
            UUID storeProductId,
            UpdateStoreProductRequest request
    ) {
        StoreProduct storeProduct = findStoreProductEntity(storeId, storeProductId);
        storeProduct.setActive(request.active());
        return toResponse(storeProductRepository.save(storeProduct));
    }

    private Category findCategoryEntity(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Category"));
    }

    private Product findProductEntity(UUID productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Product"));
    }

    private StoreProduct findStoreProductEntity(UUID storeId, UUID storeProductId) {
        ensureStore(storeId);
        return storeProductRepository.findByStore_IdAndId(storeId, storeProductId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Store product"));
    }

    private Store ensureStore(UUID storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
    }

    private Category resolveCategory(UUID categoryId) {
        return findCategoryEntity(categoryId);
    }

    private Category resolveParent(UUID parentId) {
        return parentId == null ? null : findCategoryEntity(parentId);
    }

    private void ensureCategoryCodeIsAvailable(String code, UUID categoryId) {
        boolean exists = categoryId == null
                ? categoryRepository.existsByCode(code)
                : categoryRepository.existsByCodeAndIdNot(code, categoryId);
        if (exists) {
            throw new AdminConflictException("Category code is already in use");
        }
    }

    private void ensureProductIdentifiersAreAvailable(String sku, String ean, UUID productId) {
        boolean duplicateSku = productId == null
                ? productRepository.existsBySku(sku)
                : productRepository.existsBySkuAndIdNot(sku, productId);
        if (duplicateSku) {
            throw new AdminConflictException("Product SKU is already in use");
        }

        boolean duplicateEan = ean != null && (productId == null
                ? productRepository.existsByEan(ean)
                : productRepository.existsByEanAndIdNot(ean, productId));
        if (duplicateEan) {
            throw new AdminConflictException("Product EAN is already in use");
        }
    }

    private void ensureNoCategoryCycle(UUID categoryId, Category parent) {
        if (parent == null) {
            return;
        }

        Set<UUID> visited = new HashSet<>();
        Category current = parent;
        while (current != null) {
            if (!visited.add(current.getId())) {
                throw new AdminValidationException("Category hierarchy contains a cycle");
            }
            if (current.getId().equals(categoryId)) {
                throw new AdminValidationException("Category cannot be its own ancestor");
            }
            current = current.getParent();
        }
    }

    private CategoryAdminResponse toResponse(Category category) {
        return new CategoryAdminResponse(
                category.getId(),
                category.getParent() == null ? null : category.getParent().getId(),
                category.getName(),
                category.getCode(),
                category.isActive()
        );
    }

    private ProductAdminResponse toResponse(Product product) {
        return new ProductAdminResponse(
                product.getId(),
                product.getCategory().getId(),
                product.getSku(),
                product.getEan(),
                product.getName(),
                product.getBrand(),
                product.getDescription(),
                product.isActive()
        );
    }

    private StoreProductAdminResponse toResponse(StoreProduct storeProduct) {
        return new StoreProductAdminResponse(
                storeProduct.getId(),
                storeProduct.getStore().getId(),
                storeProduct.getProduct().getId(),
                storeProduct.isActive()
        );
    }
}
