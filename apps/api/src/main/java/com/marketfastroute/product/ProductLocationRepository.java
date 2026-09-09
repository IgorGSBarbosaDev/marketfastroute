package com.marketfastroute.product;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductLocationRepository extends JpaRepository<ProductLocation, UUID> {

    @Query("""
            select location
            from ProductLocation location
            join fetch location.storeProduct storeProduct
            join fetch storeProduct.product product
            join fetch location.storeMap storeMap
            left join fetch location.sector sector
            left join fetch location.aisle aisle
            left join fetch location.shelfBlock shelfBlock
            join fetch location.navigationNode navigationNode
            where location.storeId = :storeId
              and storeProduct.store.id = :storeId
              and product.id = :productId
              and storeProduct.active = true
              and product.active = true
              and location.active = true
            """)
    List<ProductLocation> findActiveByStoreIdAndProductId(
            @Param("storeId") UUID storeId,
            @Param("productId") UUID productId
    );

    @Query("""
            select location
            from ProductLocation location
            join fetch location.storeProduct storeProduct
            join fetch storeProduct.product product
            join fetch location.storeMap storeMap
            left join fetch location.sector sector
            left join fetch location.aisle aisle
            left join fetch location.shelfBlock shelfBlock
            join fetch location.navigationNode navigationNode
            where location.storeId = :storeId
              and storeProduct.store.id = :storeId
              and product.id = :productId
              and storeProduct.active = true
              and product.active = true
              and location.active = true
              and location.primaryLocation = true
            """)
    List<ProductLocation> findActivePrimaryByStoreIdAndProductId(
            @Param("storeId") UUID storeId,
            @Param("productId") UUID productId
    );

    @Query("""
            select location
            from ProductLocation location
            join fetch location.storeProduct storeProduct
            join fetch storeProduct.product product
            join fetch location.storeMap storeMap
            left join fetch location.sector sector
            left join fetch location.aisle aisle
            left join fetch location.shelfBlock shelfBlock
            join fetch location.navigationNode navigationNode
            where location.id = :locationId
              and location.storeId = :storeId
              and storeProduct.store.id = :storeId
              and product.id = :productId
              and storeProduct.active = true
              and product.active = true
              and location.active = true
            """)
    Optional<ProductLocation> findActiveByIdAndStoreIdAndProductId(
            @Param("locationId") UUID locationId,
            @Param("storeId") UUID storeId,
            @Param("productId") UUID productId
    );
}
