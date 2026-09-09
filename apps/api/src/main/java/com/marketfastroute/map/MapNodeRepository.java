package com.marketfastroute.map;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MapNodeRepository extends JpaRepository<MapNode, UUID> {

    List<MapNode> findByStoreMap_IdAndActiveTrueOrderById(UUID mapId);
}
