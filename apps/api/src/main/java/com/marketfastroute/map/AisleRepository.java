package com.marketfastroute.map;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AisleRepository extends JpaRepository<Aisle, UUID> {

    List<Aisle> findByStoreMap_IdOrderById(UUID mapId);

    Optional<Aisle> findByStoreMap_IdAndId(UUID mapId, UUID aisleId);

    boolean existsByStoreMap_IdAndCode(UUID mapId, String code);

    boolean existsByStoreMap_IdAndCodeAndIdNot(UUID mapId, String code, UUID aisleId);

    List<Aisle> findByStoreMap_IdAndActiveTrueOrderById(UUID mapId);
}
