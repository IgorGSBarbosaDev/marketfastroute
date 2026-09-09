package com.marketfastroute.map;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PointOfInterestRepository extends JpaRepository<PointOfInterest, UUID> {

    List<PointOfInterest> findByStoreMap_IdOrderById(UUID mapId);

    Optional<PointOfInterest> findByStoreMap_IdAndId(UUID mapId, UUID pointOfInterestId);

    List<PointOfInterest> findByStoreMap_IdAndActiveTrueOrderById(UUID mapId);
}
