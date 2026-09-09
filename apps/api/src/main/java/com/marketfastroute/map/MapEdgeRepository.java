package com.marketfastroute.map;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MapEdgeRepository extends JpaRepository<MapEdge, UUID> {

    List<MapEdge> findByStoreMap_IdAndActiveTrueOrderById(UUID mapId);
}
