package com.marketfastroute.map;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ShelfBlockRepository extends JpaRepository<ShelfBlock, UUID> {

    List<ShelfBlock> findByStoreMap_IdAndActiveTrueOrderById(UUID mapId);
}
