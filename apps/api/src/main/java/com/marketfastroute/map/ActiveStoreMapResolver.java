package com.marketfastroute.map;

import com.marketfastroute.store.StoreMap;
import com.marketfastroute.store.StoreMapRepository;
import com.marketfastroute.store.StoreNotFoundException;
import com.marketfastroute.store.StoreRepository;
import org.springframework.stereotype.Component;

import java.util.Objects;
import java.util.UUID;

@Component
public class ActiveStoreMapResolver {

    private final StoreRepository storeRepository;
    private final StoreMapRepository storeMapRepository;

    public ActiveStoreMapResolver(
            StoreRepository storeRepository,
            StoreMapRepository storeMapRepository
    ) {
        this.storeRepository = storeRepository;
        this.storeMapRepository = storeMapRepository;
    }

    public StoreMap resolve(UUID storeId) {
        if (!storeRepository.existsByIdAndActiveTrue(storeId)) {
            throw new StoreNotFoundException(storeId);
        }

        StoreMap storeMap = storeMapRepository.findActiveByStoreId(storeId)
                .orElseThrow(() -> new StoreMapNotFoundException(storeId));

        if (storeMap.getStatus() != MapStatus.ACTIVE) {
            throw new StoreMapNotFoundException(storeId);
        }
        if (storeMap.getStore() == null || !Objects.equals(storeMap.getStore().getId(), storeId)) {
            throw new MapConsistencyException("Active map belongs to another store");
        }
        if (storeMap.getId() == null) {
            throw new MapConsistencyException("Active map has no identifier");
        }

        return storeMap;
    }
}
