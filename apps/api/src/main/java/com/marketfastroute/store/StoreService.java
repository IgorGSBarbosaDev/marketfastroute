package com.marketfastroute.store;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StoreService {

    private final StoreRepository storeRepository;
    private final StoreMapper storeMapper;

    public StoreService(StoreRepository storeRepository, StoreMapper storeMapper) {
        this.storeRepository = storeRepository;
        this.storeMapper = storeMapper;
    }

    public List<StoreResponse> findAll() {
        return storeRepository.findAll().stream()
                .map(storeMapper::toResponse)
                .toList();
    }

    public StoreResponse findById(UUID storeId) {
        return storeRepository.findById(storeId)
                .map(storeMapper::toResponse)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
    }
}
