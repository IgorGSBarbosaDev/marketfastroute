package com.marketfastroute.map;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SectorRepository extends JpaRepository<Sector, UUID> {

    List<Sector> findByStoreMap_IdOrderById(UUID mapId);

    Optional<Sector> findByStoreMap_IdAndId(UUID mapId, UUID sectorId);

    boolean existsByStoreMap_IdAndCode(UUID mapId, String code);

    boolean existsByStoreMap_IdAndCodeAndIdNot(UUID mapId, String code, UUID sectorId);

    List<Sector> findByStoreMap_IdAndActiveTrueOrderById(UUID mapId);
}
