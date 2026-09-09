package com.marketfastroute.map;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AisleRepository extends JpaRepository<Aisle, UUID> {

    List<Aisle> findByStoreMap_IdAndActiveTrueOrderById(UUID mapId);
}
