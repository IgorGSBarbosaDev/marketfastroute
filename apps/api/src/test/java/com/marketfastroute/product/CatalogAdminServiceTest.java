package com.marketfastroute.product;

import com.marketfastroute.admin.AdminConflictException;
import com.marketfastroute.admin.AdminResourceNotFoundException;
import com.marketfastroute.admin.AdminValidationException;
import com.marketfastroute.admin.dto.CreateCategoryRequest;
import com.marketfastroute.admin.dto.CreateProductRequest;
import com.marketfastroute.admin.dto.CreateStoreProductRequest;
import com.marketfastroute.admin.dto.UpdateCategoryRequest;
import com.marketfastroute.store.Store;
import com.marketfastroute.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CatalogAdminServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreProductRepository storeProductRepository;

    private CatalogAdminService service;

    @BeforeEach
    void setUp() {
        service = new CatalogAdminService(
                categoryRepository, productRepository, storeRepository, storeProductRepository);
    }

    @Test
    void rejectsDuplicateCategoryCode() {
        when(categoryRepository.existsByCode("DAIRY")).thenReturn(true);

        assertThrows(AdminConflictException.class,
                () -> service.createCategory(new CreateCategoryRequest("Dairy", "DAIRY", null)));
    }

    @Test
    void rejectsCategoryCyclesOnUpdate() {
        UUID categoryId = UUID.randomUUID();
        UUID parentId = UUID.randomUUID();
        Category category = category(categoryId, "ROOT", null);
        Category parent = category(parentId, "PARENT", category);

        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(categoryRepository.existsByCodeAndIdNot("ROOT-2", categoryId)).thenReturn(false);
        when(categoryRepository.findById(parentId)).thenReturn(Optional.of(parent));

        assertThrows(AdminValidationException.class, () -> service.updateCategory(
                categoryId,
                new UpdateCategoryRequest("Root", "ROOT-2", parentId, true)));
    }

    @Test
    void rejectsProductWithMissingCategory() {
        UUID categoryId = UUID.randomUUID();
        when(productRepository.existsBySku("SKU-1")).thenReturn(false);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());

        assertThrows(AdminResourceNotFoundException.class, () -> service.createProduct(
                new CreateProductRequest(categoryId, "SKU-1", null, "Milk", null, null)));
    }

    @Test
    void rejectsStoreProductForMissingProduct() {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Store store = org.mockito.Mockito.mock(Store.class);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(AdminResourceNotFoundException.class, () -> service.createStoreProduct(
                storeId, new CreateStoreProductRequest(productId, true)));
    }

    @Test
    void rejectsDuplicateStoreProductAssociation() {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Store store = org.mockito.Mockito.mock(Store.class);
        Product product = product(productId);
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(productRepository.findById(productId)).thenReturn(Optional.of(product));
        when(storeProductRepository.existsByStore_IdAndProduct_Id(storeId, productId)).thenReturn(true);

        assertThrows(AdminConflictException.class, () -> service.createStoreProduct(
                storeId, new CreateStoreProductRequest(productId, true)));
    }

    private Category category(UUID id, String code, Category parent) {
        Category category = new Category();
        category.setId(id);
        category.setName(code);
        category.setCode(code);
        category.setParent(parent);
        return category;
    }

    private Product product(UUID id) {
        Product product = new Product();
        product.setId(id);
        product.setSku("SKU-" + id);
        product.setName("Product");
        return product;
    }
}
