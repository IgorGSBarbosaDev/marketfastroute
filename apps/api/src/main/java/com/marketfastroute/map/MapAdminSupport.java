package com.marketfastroute.map;

import com.marketfastroute.admin.AdminResourceNotFoundException;
import com.marketfastroute.store.StoreMap;
import com.marketfastroute.store.StoreMapRepository;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class MapAdminSupport {

    private final StoreMapRepository storeMapRepository;

    public MapAdminSupport(StoreMapRepository storeMapRepository) {
        this.storeMapRepository = storeMapRepository;
    }

    public StoreMap findMap(UUID mapId) {
        return storeMapRepository.findById(mapId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Store map"));
    }

    public Sector findSector(UUID mapId, UUID sectorId, SectorRepository repository) {
        return repository.findByStoreMap_IdAndId(mapId, sectorId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Sector"));
    }

    public Aisle findAisle(UUID mapId, UUID aisleId, AisleRepository repository) {
        return repository.findByStoreMap_IdAndId(mapId, aisleId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Aisle"));
    }

    public ShelfBlock findShelfBlock(UUID mapId, UUID shelfBlockId, ShelfBlockRepository repository) {
        return repository.findByStoreMap_IdAndId(mapId, shelfBlockId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Shelf block"));
    }
}
