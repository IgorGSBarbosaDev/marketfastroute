package com.marketfastroute.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;
import java.util.UUID;

public interface StoreMapRepository extends JpaRepository<StoreMap, UUID> {

    List<StoreMap> findByStore_IdOrderByVersion(UUID storeId);

    Optional<StoreMap> findByStore_IdAndId(UUID storeId, UUID mapId);

    boolean existsByStore_IdAndVersion(UUID storeId, int version);

    boolean existsByStore_IdAndVersionAndIdNot(UUID storeId, int version, UUID mapId);

    @Query("""
            select storeMap
            from StoreMap storeMap
            where storeMap.store.id = :storeId
              and storeMap.status = com.marketfastroute.map.MapStatus.ACTIVE
            """)
    Optional<StoreMap> findActiveByStoreId(@Param("storeId") UUID storeId);
}
