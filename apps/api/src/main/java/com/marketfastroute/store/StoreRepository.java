package com.marketfastroute.store;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface StoreRepository extends JpaRepository<Store, UUID> {

    boolean existsByCode(String code);

    boolean existsByCodeAndIdNot(String code, UUID id);

    List<Store> findByActiveTrueOrderByNameAsc();

    Optional<Store> findByIdAndActiveTrue(UUID storeId);

    boolean existsByIdAndActiveTrue(UUID storeId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select store from Store store where store.id = :storeId")
    Optional<Store> findByIdForUpdate(@Param("storeId") UUID storeId);
}
