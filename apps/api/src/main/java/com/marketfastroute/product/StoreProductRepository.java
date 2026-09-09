package com.marketfastroute.product;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreProductRepository extends JpaRepository<StoreProduct, UUID> {

    @Query("""
            select storeProduct
            from StoreProduct storeProduct
            join fetch storeProduct.product product
            join fetch product.category category
            where storeProduct.store.id = :storeId
              and storeProduct.active = true
              and product.active = true
            """)
    List<StoreProduct> findAvailableByStoreId(@Param("storeId") UUID storeId);

    @Query("""
            select storeProduct
            from StoreProduct storeProduct
            join fetch storeProduct.product product
            join fetch product.category category
            where storeProduct.store.id = :storeId
              and product.id = :productId
              and storeProduct.active = true
              and product.active = true
            """)
    Optional<StoreProduct> findAvailableByStoreIdAndProductId(
            @Param("storeId") UUID storeId,
            @Param("productId") UUID productId
    );

    @Query("""
            select storeProduct
            from StoreProduct storeProduct
            join fetch storeProduct.product product
            join fetch product.category category
            where storeProduct.store.id = :storeId
              and storeProduct.active = true
              and product.active = true
              and (
                    lower(product.name) like lower(concat('%', :term, '%'))
                    or lower(product.sku) like lower(concat('%', :term, '%'))
                    or lower(coalesce(product.ean, '')) like lower(concat('%', :term, '%'))
                    or lower(category.name) like lower(concat('%', :term, '%'))
                    or lower(category.code) like lower(concat('%', :term, '%'))
              )
            """)
    List<StoreProduct> searchAvailableByStoreId(
            @Param("storeId") UUID storeId,
            @Param("term") String term
    );
}
