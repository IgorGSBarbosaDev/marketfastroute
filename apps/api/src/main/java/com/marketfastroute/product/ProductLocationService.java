package com.marketfastroute.product;

import com.marketfastroute.map.Aisle;
import com.marketfastroute.map.MapNode;
import com.marketfastroute.map.Sector;
import com.marketfastroute.map.ShelfBlock;
import com.marketfastroute.store.StoreMap;
import com.marketfastroute.store.StoreNotFoundException;
import com.marketfastroute.store.StoreRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductLocationService {

    private final StoreRepository storeRepository;
    private final StoreProductRepository storeProductRepository;
    private final ProductLocationRepository productLocationRepository;
    private final ProductLocationMapper productLocationMapper;

    public ProductLocationService(
            StoreRepository storeRepository,
            StoreProductRepository storeProductRepository,
            ProductLocationRepository productLocationRepository,
            ProductLocationMapper productLocationMapper
    ) {
        this.storeRepository = storeRepository;
        this.storeProductRepository = storeProductRepository;
        this.productLocationRepository = productLocationRepository;
        this.productLocationMapper = productLocationMapper;
    }

    public List<ProductLocationResponse> findByProduct(UUID storeId, UUID productId) {
        ensureStoreExists(storeId);
        ensureProductIsAvailableInStore(storeId, productId);

        return productLocationRepository.findActiveByStoreIdAndProductId(storeId, productId).stream()
                .filter(ProductLocation::isActive)
                .map(location -> toResponseAfterValidation(location, storeId, productId))
                .toList();
    }

    public ProductLocationResponse findPrimaryByProduct(UUID storeId, UUID productId) {
        ensureStoreExists(storeId);
        ensureProductIsAvailableInStore(storeId, productId);

        List<ProductLocation> primaryLocations = productLocationRepository
                .findActivePrimaryByStoreIdAndProductId(storeId, productId);

        primaryLocations.forEach(location -> validateConsistency(location, storeId, productId));
        if (primaryLocations.isEmpty()) {
            throw new ProductLocationNotFoundException(storeId, productId);
        }
        if (primaryLocations.size() > 1) {
            throw new ProductLocationConsistencyException(
                    primaryLocations.getFirst().getId(),
                    "more than one primary location exists for the product"
            );
        }

        return productLocationMapper.toResponse(primaryLocations.getFirst());
    }

    public ProductLocationResponse findById(
            UUID storeId,
            UUID productId,
            UUID locationId
    ) {
        ensureStoreExists(storeId);
        ensureProductIsAvailableInStore(storeId, productId);

        ProductLocation location = productLocationRepository
                .findActiveByIdAndStoreIdAndProductId(locationId, storeId, productId)
                .orElseThrow(() -> new ProductLocationNotFoundException(storeId, productId, locationId));

        validateConsistency(location, storeId, productId);
        return productLocationMapper.toResponse(location);
    }

    private void ensureProductIsAvailableInStore(UUID storeId, UUID productId) {
        StoreProduct storeProduct = storeProductRepository.findAvailableByStoreIdAndProductId(storeId, productId)
                .orElseThrow(() -> new ProductNotFoundException(storeId, productId));
        if (!storeProduct.isActive()
                || storeProduct.getProduct() == null
                || !storeProduct.getProduct().isActive()) {
            throw new ProductNotFoundException(storeId, productId);
        }
    }

    private ProductLocationResponse toResponseAfterValidation(
            ProductLocation location,
            UUID storeId,
            UUID productId
    ) {
        validateConsistency(location, storeId, productId);
        return productLocationMapper.toResponse(location);
    }

    private void ensureStoreExists(UUID storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new StoreNotFoundException(storeId);
        }
    }

    private void validateConsistency(ProductLocation location, UUID storeId, UUID productId) {
        StoreProduct storeProduct = location.getStoreProduct();
        StoreMap storeMap = location.getStoreMap();

        if (!Objects.equals(location.getStoreId(), storeId)) {
            inconsistent(location, "store_id does not match the requested store");
        }
        if (storeProduct == null || storeProduct.getStore() == null
                || !Objects.equals(location.getStoreProductId(), storeProduct.getId())
                || !Objects.equals(storeProduct.getStore().getId(), storeId)) {
            inconsistent(location, "store product belongs to another store");
        }
        if (storeProduct == null || storeProduct.getProduct() == null
                || !Objects.equals(storeProduct.getProduct().getId(), productId)) {
            inconsistent(location, "store product references another product");
        }
        if (storeMap == null || !Objects.equals(location.getMapId(), storeMap.getId())) {
            inconsistent(location, "map reference does not match map_id");
        }
        if (storeMap.getStore() == null || !Objects.equals(storeMap.getStore().getId(), storeId)) {
            inconsistent(location, "map belongs to another store");
        }

        validateMapReference(location, location.getSectorId(), location.getSector(), "sector");
        validateMapReference(location, location.getAisleId(), location.getAisle(), "aisle");
        validateMapReference(location, location.getShelfBlockId(), location.getShelfBlock(), "shelf block");
        validateMapReference(location, location.getNavigationNodeId(), location.getNavigationNode(), "navigation node");
        validateHierarchy(location);
    }

    private void validateMapReference(
            ProductLocation location,
            UUID referenceId,
            Object reference,
            String referenceName
    ) {
        if (referenceId == null) {
            if (reference != null) {
                inconsistent(location, referenceName + " is loaded without an identifier");
            }
            return;
        }

        UUID referenceMapId = switch (reference) {
            case Sector sector -> sector.getStoreMap() == null ? null : sector.getStoreMap().getId();
            case Aisle aisle -> aisle.getStoreMap() == null ? null : aisle.getStoreMap().getId();
            case ShelfBlock shelfBlock -> shelfBlock.getStoreMap() == null ? null : shelfBlock.getStoreMap().getId();
            case MapNode mapNode -> mapNode.getStoreMap() == null ? null : mapNode.getStoreMap().getId();
            case null, default -> null;
        };

        if (reference == null || !Objects.equals(referenceId, getReferenceId(reference))
                || !Objects.equals(location.getMapId(), referenceMapId)) {
            inconsistent(location, referenceName + " does not belong to the location map");
        }
    }

    private UUID getReferenceId(Object reference) {
        return switch (reference) {
            case Sector sector -> sector.getId();
            case Aisle aisle -> aisle.getId();
            case ShelfBlock shelfBlock -> shelfBlock.getId();
            case MapNode mapNode -> mapNode.getId();
            case null, default -> null;
        };
    }

    private void validateHierarchy(ProductLocation location) {
        Sector sector = location.getSector();
        Aisle aisle = location.getAisle();
        ShelfBlock shelfBlock = location.getShelfBlock();

        if (sector != null && aisle != null && aisle.getSectorId() != null
                && !Objects.equals(aisle.getSectorId(), sector.getId())) {
            inconsistent(location, "aisle is not associated with the selected sector");
        }
        if (sector != null && shelfBlock != null && shelfBlock.getSectorId() != null
                && !Objects.equals(shelfBlock.getSectorId(), sector.getId())) {
            inconsistent(location, "shelf block is not associated with the selected sector");
        }
        if (aisle != null && shelfBlock != null && shelfBlock.getAisleId() != null
                && !Objects.equals(shelfBlock.getAisleId(), aisle.getId())) {
            inconsistent(location, "shelf block is not associated with the selected aisle");
        }
    }

    private void inconsistent(ProductLocation location, String reason) {
        throw new ProductLocationConsistencyException(location.getId(), reason);
    }
}
