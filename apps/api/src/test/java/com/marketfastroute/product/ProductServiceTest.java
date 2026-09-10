package com.marketfastroute.product;

import com.marketfastroute.store.StoreNotFoundException;
import com.marketfastroute.store.StoreRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreProductRepository storeProductRepository;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        productService = new ProductService(storeRepository, storeProductRepository, new ProductMapper());
    }

    @Test
    void listsAvailableProductsForAnExistingStore() {
        UUID storeId = UUID.randomUUID();
        Product product = newProduct("Milk", "SKU-MILK", true);
        StoreProduct storeProduct = newStoreProduct(product, true);

        when(storeRepository.existsByIdAndActiveTrue(storeId)).thenReturn(true);
        when(storeProductRepository.findAvailableByStoreId(storeId)).thenReturn(List.of(storeProduct));

        List<ProductResponse> result = productService.findAvailableByStore(storeId, null);

        assertEquals(List.of(productResponse(product)), result);
        verify(storeRepository).existsByIdAndActiveTrue(storeId);
        verify(storeProductRepository).findAvailableByStoreId(storeId);
    }

    @Test
    void findsAProductAvailableInTheRequestedStore() {
        UUID storeId = UUID.randomUUID();
        Product product = newProduct("Milk", "SKU-MILK", true);
        StoreProduct storeProduct = newStoreProduct(product, true);

        when(storeRepository.existsByIdAndActiveTrue(storeId)).thenReturn(true);
        when(storeProductRepository.findAvailableByStoreIdAndProductId(storeId, product.getId()))
                .thenReturn(Optional.of(storeProduct));

        ProductResponse result = productService.findById(storeId, product.getId());

        assertEquals(productResponse(product), result);
        verify(storeProductRepository).findAvailableByStoreIdAndProductId(storeId, product.getId());
    }

    @Test
    void searchesProductsUsingTheTrimmedTerm() {
        UUID storeId = UUID.randomUUID();
        Product product = newProduct("Milk", "SKU-MILK", true);
        StoreProduct storeProduct = newStoreProduct(product, true);

        when(storeRepository.existsByIdAndActiveTrue(storeId)).thenReturn(true);
        when(storeProductRepository.searchAvailableByStoreId(storeId, "milk"))
                .thenReturn(List.of(storeProduct));

        List<ProductResponse> result = productService.findAvailableByStore(storeId, "  milk  ");

        assertEquals(List.of(productResponse(product)), result);
        verify(storeProductRepository).searchAvailableByStoreId(storeId, "milk");
        verify(storeProductRepository, never()).findAvailableByStoreId(storeId);
    }

    @Test
    void rejectsRequestsForANonexistentStore() {
        UUID storeId = UUID.randomUUID();
        when(storeRepository.existsByIdAndActiveTrue(storeId)).thenReturn(false);

        assertThrows(
                StoreNotFoundException.class,
                () -> productService.findAvailableByStore(storeId, null)
        );

        verifyNoInteractions(storeProductRepository);
    }

    @Test
    void doesNotReturnAProductThatIsNotAssociatedWithTheStore() {
        UUID storeId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(storeRepository.existsByIdAndActiveTrue(storeId)).thenReturn(true);
        when(storeProductRepository.findAvailableByStoreIdAndProductId(storeId, productId))
                .thenReturn(Optional.empty());

        assertThrows(
                ProductNotFoundException.class,
                () -> productService.findById(storeId, productId)
        );
    }

    @Test
    void doesNotReturnAnInactiveProduct() {
        UUID storeId = UUID.randomUUID();
        Product product = newProduct("Milk", "SKU-MILK", false);
        StoreProduct storeProduct = newStoreProduct(product, true);
        when(storeRepository.existsByIdAndActiveTrue(storeId)).thenReturn(true);
        when(storeProductRepository.findAvailableByStoreId(storeId)).thenReturn(List.of(storeProduct));

        assertTrue(productService.findAvailableByStore(storeId, null).isEmpty());
    }

    @Test
    void doesNotReturnAnInactiveStoreProduct() {
        UUID storeId = UUID.randomUUID();
        Product product = newProduct("Milk", "SKU-MILK", true);
        StoreProduct storeProduct = newStoreProduct(product, false);
        when(storeRepository.existsByIdAndActiveTrue(storeId)).thenReturn(true);
        when(storeProductRepository.findAvailableByStoreId(storeId)).thenReturn(List.of(storeProduct));

        assertTrue(productService.findAvailableByStore(storeId, null).isEmpty());
    }

    private Product newProduct(String name, String sku, boolean active) {
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Dairy");
        category.setCode("DAIRY");

        Product product = new Product();
        product.setId(UUID.randomUUID());
        product.setCategory(category);
        product.setName(name);
        product.setSku(sku);
        product.setEan("789" + sku.replace("SKU-", ""));
        product.setBrand("Brand");
        product.setActive(active);
        return product;
    }

    private StoreProduct newStoreProduct(Product product, boolean active) {
        StoreProduct storeProduct = new StoreProduct();
        storeProduct.setId(UUID.randomUUID());
        storeProduct.setProduct(product);
        storeProduct.setActive(active);
        return storeProduct;
    }

    private ProductResponse productResponse(Product product) {
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
