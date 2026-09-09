package com.marketfastroute.map;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MapNodeRepository extends JpaRepository<MapNode, UUID> {

    List<MapNode> findByStoreMap_IdOrderById(UUID mapId);

    Optional<MapNode> findByStoreMap_IdAndId(UUID mapId, UUID nodeId);

    List<MapNode> findByStoreMap_IdAndActiveTrueOrderById(UUID mapId);
}
