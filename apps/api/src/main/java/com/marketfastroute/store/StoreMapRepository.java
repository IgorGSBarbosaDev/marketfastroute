package com.marketfastroute.store;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StoreMapRepository extends JpaRepository<StoreMap, UUID> {

    @Query("""
            select storeMap
            from StoreMap storeMap
            where storeMap.store.id = :storeId
              and storeMap.status = com.marketfastroute.map.MapStatus.ACTIVE
            """)
    Optional<StoreMap> findActiveByStoreId(@Param("storeId") UUID storeId);
}
