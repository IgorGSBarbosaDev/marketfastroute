package com.marketfastroute.store;

import com.marketfastroute.admin.AdminConflictException;
import com.marketfastroute.admin.dto.CreateStoreRequest;
import com.marketfastroute.admin.dto.StoreAdminResponse;
import com.marketfastroute.admin.dto.UpdateStoreRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class StoreAdminService {

    private final StoreRepository storeRepository;

    public StoreAdminService(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    public List<StoreAdminResponse> findAll() {
        return storeRepository.findAll().stream().map(this::toResponse).toList();
    }

    public StoreAdminResponse findById(UUID storeId) {
        return toResponse(findStore(storeId));
    }

    @Transactional
    public StoreAdminResponse create(CreateStoreRequest request) {
        if (storeRepository.existsByCode(request.code())) {
            throw new AdminConflictException("Store code is already in use");
        }

        Store store = new Store();
        apply(store, request);
        return toResponse(storeRepository.save(store));
    }

    @Transactional
    public StoreAdminResponse update(UUID storeId, UpdateStoreRequest request) {
        Store store = findStore(storeId);
        if (storeRepository.existsByCodeAndIdNot(request.code(), storeId)) {
            throw new AdminConflictException("Store code is already in use");
        }

        apply(store, request);
        return toResponse(storeRepository.save(store));
    }

    private Store findStore(UUID storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
    }

    private void apply(Store store, CreateStoreRequest request) {
        store.setName(request.name());
        store.setCode(request.code());
        store.setAddress(request.address());
        store.setCity(request.city());
        store.setState(request.state());
    }

    private void apply(Store store, UpdateStoreRequest request) {
        apply(store, new CreateStoreRequest(
                request.name(), request.code(), request.address(), request.city(), request.state()));
        store.setActive(request.active());
    }

    private StoreAdminResponse toResponse(Store store) {
        return new StoreAdminResponse(
                store.getId(),
                store.getName(),
                store.getCode(),
                store.getAddress(),
                store.getCity(),
                store.getState(),
                store.isActive()
        );
    }
}
