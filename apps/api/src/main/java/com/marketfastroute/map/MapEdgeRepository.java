package com.marketfastroute.map;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MapEdgeRepository extends JpaRepository<MapEdge, UUID> {

    List<MapEdge> findByStoreMap_IdOrderById(UUID mapId);

    Optional<MapEdge> findByStoreMap_IdAndId(UUID mapId, UUID edgeId);

    boolean existsByStoreMap_IdAndFromNodeIdAndToNodeId(UUID mapId, UUID fromNodeId, UUID toNodeId);

    boolean existsByStoreMap_IdAndFromNodeIdAndToNodeIdAndIdNot(
            UUID mapId, UUID fromNodeId, UUID toNodeId, UUID edgeId);

    List<MapEdge> findByStoreMap_IdAndActiveTrueOrderById(UUID mapId);
}
