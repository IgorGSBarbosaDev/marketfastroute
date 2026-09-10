package com.marketfastroute.store;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    List<Store> findByActiveTrueOrderByNameAsc();

    Optional<Store> findByIdAndActiveTrue(UUID storeId);

    boolean existsByIdAndActiveTrue(UUID storeId);
}
