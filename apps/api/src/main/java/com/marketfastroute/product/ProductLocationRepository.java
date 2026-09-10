package com.marketfastroute.product;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductLocationRepository extends JpaRepository<ProductLocation, UUID> {

    List<ProductLocation> findByStoreIdOrderById(UUID storeId);

    Optional<ProductLocation> findByStoreIdAndId(UUID storeId, UUID locationId);

    boolean existsByStoreIdAndStoreProductIdAndMapIdAndPrimaryLocationTrue(
            UUID storeId, UUID storeProductId, UUID mapId);

    boolean existsByStoreIdAndStoreProductIdAndMapIdAndPrimaryLocationTrueAndIdNot(
            UUID storeId, UUID storeProductId, UUID mapId, UUID locationId);

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
              and location.mapId = :mapId
              and storeProduct.store.id = :storeId
              and product.id = :productId
              and storeProduct.active = true
              and product.active = true
              and location.active = true
              and storeMap.id = :mapId
              and storeMap.store.id = :storeId
            """)
    List<ProductLocation> findActiveByStoreIdAndMapIdAndProductId(
            @Param("storeId") UUID storeId,
            @Param("mapId") UUID mapId,
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
              and location.mapId = :mapId
              and storeProduct.store.id = :storeId
              and product.id = :productId
              and storeProduct.active = true
              and product.active = true
              and location.active = true
              and storeMap.id = :mapId
              and storeMap.store.id = :storeId
              and location.primaryLocation = true
            """)
    List<ProductLocation> findActivePrimaryByStoreIdAndMapId(
            @Param("storeId") UUID storeId,
            @Param("mapId") UUID mapId,
            @Param("productId") UUID productId
    );

    @Query("""
            select location
            from ProductLocation location
            join fetch location.storeProduct storeProduct
            join fetch storeProduct.product product
            join fetch location.storeMap storeMap
            join fetch location.navigationNode navigationNode
            where location.storeId = :storeId
              and location.mapId = :mapId
              and storeProduct.store.id = :storeId
              and product.id in :productIds
              and storeProduct.active = true
              and product.active = true
              and location.active = true
              and storeMap.id = :mapId
              and storeMap.store.id = :storeId
              and navigationNode.active = true
              and navigationNode.storeMap.id = :mapId
            order by product.id, location.primaryLocation desc, location.id
            """)
    List<ProductLocation> findActiveByStoreIdAndMapIdAndProductIdIn(
            @Param("storeId") UUID storeId,
            @Param("mapId") UUID mapId,
            @Param("productIds") List<UUID> productIds
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
              and location.mapId = :mapId
              and storeProduct.store.id = :storeId
              and product.id = :productId
              and storeProduct.active = true
              and product.active = true
              and location.active = true
              and storeMap.id = :mapId
              and storeMap.store.id = :storeId
            """)
    Optional<ProductLocation> findActiveByIdAndStoreIdAndMapIdAndProductId(
            @Param("locationId") UUID locationId,
            @Param("storeId") UUID storeId,
            @Param("mapId") UUID mapId,
            @Param("productId") UUID productId
    );
}
