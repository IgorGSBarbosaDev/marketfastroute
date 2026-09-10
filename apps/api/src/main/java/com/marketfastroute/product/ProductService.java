package com.marketfastroute.product;

import com.marketfastroute.store.StoreNotFoundException;
import com.marketfastroute.store.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductService {

    private final StoreRepository storeRepository;
    private final StoreProductRepository storeProductRepository;
    private final ProductMapper productMapper;

    public ProductService(
            StoreRepository storeRepository,
            StoreProductRepository storeProductRepository,
            ProductMapper productMapper
    ) {
        this.storeRepository = storeRepository;
        this.storeProductRepository = storeProductRepository;
        this.productMapper = productMapper;
    }

    public List<ProductResponse> findAvailableByStore(UUID storeId, String search) {
        ensureStoreExists(storeId);

        List<StoreProduct> storeProducts = search == null || search.isBlank()
                ? storeProductRepository.findAvailableByStoreId(storeId)
                : storeProductRepository.searchAvailableByStoreId(storeId, search.trim());

        return storeProducts.stream()
                .filter(this::isAvailable)
                .map(StoreProduct::getProduct)
                .map(productMapper::toResponse)
                .toList();
    }

    public ProductResponse findById(UUID storeId, UUID productId) {
        ensureStoreExists(storeId);

        return storeProductRepository.findAvailableByStoreIdAndProductId(storeId, productId)
                .filter(this::isAvailable)
                .map(StoreProduct::getProduct)
                .map(productMapper::toResponse)
                .orElseThrow(() -> new ProductNotFoundException(storeId, productId));
    }

    private void ensureStoreExists(UUID storeId) {
        if (!storeRepository.existsByIdAndActiveTrue(storeId)) {
            throw new StoreNotFoundException(storeId);
        }
    }

    private boolean isAvailable(StoreProduct storeProduct) {
        return storeProduct.isActive() && storeProduct.getProduct().isActive();
    }
}
