package com.marketfastroute.map;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShelfBlockRepository extends JpaRepository<ShelfBlock, UUID> {

    List<ShelfBlock> findByStoreMap_IdOrderById(UUID mapId);

    Optional<ShelfBlock> findByStoreMap_IdAndId(UUID mapId, UUID shelfBlockId);

    boolean existsByStoreMap_IdAndCode(UUID mapId, String code);

    boolean existsByStoreMap_IdAndCodeAndIdNot(UUID mapId, String code, UUID shelfBlockId);

    List<ShelfBlock> findByStoreMap_IdAndActiveTrueOrderById(UUID mapId);
}
