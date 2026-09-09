package com.marketfastroute.store;

import org.springframework.stereotype.Component;

@Component
public class StoreMapper {

    public StoreResponse toResponse(Store store) {
        return new StoreResponse(
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
