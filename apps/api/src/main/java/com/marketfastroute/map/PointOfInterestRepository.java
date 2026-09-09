package com.marketfastroute.map;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface PointOfInterestRepository extends JpaRepository<PointOfInterest, UUID> {

    List<PointOfInterest> findByStoreMap_IdAndActiveTrueOrderById(UUID mapId);

    @Query("""
            select pointOfInterest
            from PointOfInterest pointOfInterest
            join fetch pointOfInterest.navigationNode navigationNode
            where pointOfInterest.storeMap.id = :mapId
              and pointOfInterest.type = :type
              and pointOfInterest.active = true
              and navigationNode.active = true
              and navigationNode.storeMap.id = :mapId
            order by pointOfInterest.id
            """)
    List<PointOfInterest> findActiveNavigableByMapIdAndType(
            @Param("mapId") UUID mapId,
            @Param("type") PointOfInterestType type
    );
}
