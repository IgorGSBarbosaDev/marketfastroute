package com.marketfastroute.product;

import com.marketfastroute.admin.AdminConflictException;
import com.marketfastroute.admin.AdminResourceNotFoundException;
import com.marketfastroute.admin.AdminValidationException;
import com.marketfastroute.admin.dto.CreateProductLocationRequest;
import com.marketfastroute.admin.dto.ProductLocationAdminResponse;
import com.marketfastroute.admin.dto.UpdateProductLocationRequest;
import com.marketfastroute.map.Aisle;
import com.marketfastroute.map.AisleRepository;
import com.marketfastroute.map.MapNode;
import com.marketfastroute.map.MapNodeRepository;
import com.marketfastroute.map.Sector;
import com.marketfastroute.map.SectorRepository;
import com.marketfastroute.map.ShelfBlock;
import com.marketfastroute.map.ShelfBlockRepository;
import com.marketfastroute.store.StoreMap;
import com.marketfastroute.store.StoreMapRepository;
import com.marketfastroute.store.StoreNotFoundException;
import com.marketfastroute.store.StoreRepository;
import com.marketfastroute.store.Store;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ProductLocationAdminService {

    private final StoreRepository storeRepository;
    private final StoreProductRepository storeProductRepository;
    private final StoreMapRepository storeMapRepository;
    private final ProductLocationRepository productLocationRepository;
    private final SectorRepository sectorRepository;
    private final AisleRepository aisleRepository;
    private final ShelfBlockRepository shelfBlockRepository;
    private final MapNodeRepository mapNodeRepository;

    public ProductLocationAdminService(
            StoreRepository storeRepository,
            StoreProductRepository storeProductRepository,
            StoreMapRepository storeMapRepository,
            ProductLocationRepository productLocationRepository,
            SectorRepository sectorRepository,
            AisleRepository aisleRepository,
            ShelfBlockRepository shelfBlockRepository,
            MapNodeRepository mapNodeRepository
    ) {
        this.storeRepository = storeRepository;
        this.storeProductRepository = storeProductRepository;
        this.storeMapRepository = storeMapRepository;
        this.productLocationRepository = productLocationRepository;
        this.sectorRepository = sectorRepository;
        this.aisleRepository = aisleRepository;
        this.shelfBlockRepository = shelfBlockRepository;
        this.mapNodeRepository = mapNodeRepository;
    }

    public List<ProductLocationAdminResponse> findByStore(UUID storeId) {
        ensureStore(storeId);
        return productLocationRepository.findByStoreIdOrderById(storeId).stream().map(this::toResponse).toList();
    }

    public ProductLocationAdminResponse findById(UUID storeId, UUID locationId) {
        return toResponse(findLocation(storeId, locationId));
    }

    @Transactional
    public ProductLocationAdminResponse create(UUID storeId, CreateProductLocationRequest request) {
        ensureStore(storeId);
        ensurePrimaryLocationIsValid(request.primaryLocation(), request.active());
        ResolvedReferences references = resolveReferences(
                storeId,
                request.storeProductId(),
                request.mapId(),
                request.sectorId(),
                request.aisleId(),
                request.shelfBlockId(),
                request.navigationNodeId());
        ensurePrimaryIsUnique(storeId, request.storeProductId(), request.mapId(), null, request.primaryLocation());

        ProductLocation location = new ProductLocation();
        location.setStoreId(storeId);
        apply(location, request, references);
        return toResponse(productLocationRepository.save(location));
    }

    @Transactional
    public ProductLocationAdminResponse update(
            UUID storeId,
            UUID locationId,
            UpdateProductLocationRequest request
    ) {
        ProductLocation location = findLocation(storeId, locationId);
        ensurePrimaryLocationIsValid(request.primaryLocation(), request.active());
        ResolvedReferences references = resolveReferences(
                storeId,
                request.storeProductId(),
                request.mapId(),
                request.sectorId(),
                request.aisleId(),
                request.shelfBlockId(),
                request.navigationNodeId());
        ensurePrimaryIsUnique(storeId, request.storeProductId(), request.mapId(), locationId, request.primaryLocation());

        apply(location, request, references);
        return toResponse(productLocationRepository.save(location));
    }

    private ProductLocation findLocation(UUID storeId, UUID locationId) {
        ensureStore(storeId);
        return productLocationRepository.findByStoreIdAndId(storeId, locationId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Product location"));
    }

    private StoreProduct ensureStoreProduct(UUID storeId, UUID storeProductId) {
        return storeProductRepository.findByStore_IdAndId(storeId, storeProductId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Store product"));
    }

    private StoreMap ensureStoreMap(UUID storeId, UUID mapId) {
        return storeMapRepository.findByStore_IdAndId(storeId, mapId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Store map"));
    }

    private ResolvedReferences resolveReferences(
            UUID storeId,
            UUID storeProductId,
            UUID mapId,
            UUID sectorId,
            UUID aisleId,
            UUID shelfBlockId,
            UUID navigationNodeId
    ) {
        ensureStoreProduct(storeId, storeProductId);
        ensureStoreMap(storeId, mapId);

        Sector sector = sectorId == null ? null : sectorRepository.findByStoreMap_IdAndId(mapId, sectorId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Sector"));
        Aisle aisle = aisleId == null ? null : aisleRepository.findByStoreMap_IdAndId(mapId, aisleId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Aisle"));
        ShelfBlock shelfBlock = shelfBlockId == null ? null : shelfBlockRepository.findByStoreMap_IdAndId(mapId, shelfBlockId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Shelf block"));
        MapNode node = mapNodeRepository.findByStoreMap_IdAndId(mapId, navigationNodeId)
                .orElseThrow(() -> new AdminResourceNotFoundException("Map node"));

        validateHierarchy(sector, aisle, shelfBlock);
        return new ResolvedReferences(sector, aisle, shelfBlock, node);
    }

    private void validateHierarchy(Sector sector, Aisle aisle, ShelfBlock shelfBlock) {
        if (sector != null && aisle != null && !Objects.equals(aisle.getSectorId(), sector.getId())) {
            throw new AdminValidationException("Aisle does not belong to the selected sector");
        }
        if (sector != null && shelfBlock != null && !Objects.equals(shelfBlock.getSectorId(), sector.getId())) {
            throw new AdminValidationException("Shelf block does not belong to the selected sector");
        }
        if (aisle != null && shelfBlock != null && !Objects.equals(shelfBlock.getAisleId(), aisle.getId())) {
            throw new AdminValidationException("Shelf block does not belong to the selected aisle");
        }
    }

    private void ensurePrimaryLocationIsValid(boolean primaryLocation, boolean active) {
        if (primaryLocation && !active) {
            throw new AdminValidationException("An inactive product location cannot be primary");
        }
    }

    private void ensurePrimaryIsUnique(
            UUID storeId,
            UUID storeProductId,
            UUID mapId,
            UUID locationId,
            boolean primaryLocation
    ) {
        if (!primaryLocation) {
            return;
        }
        boolean exists = locationId == null
                ? productLocationRepository.existsByStoreIdAndStoreProductIdAndMapIdAndPrimaryLocationTrue(
                storeId, storeProductId, mapId)
                : productLocationRepository.existsByStoreIdAndStoreProductIdAndMapIdAndPrimaryLocationTrueAndIdNot(
                storeId, storeProductId, mapId, locationId);
        if (exists) {
            throw new AdminConflictException("A primary product location already exists for the product and map");
        }
    }

    private void apply(ProductLocation location, CreateProductLocationRequest request, ResolvedReferences references) {
        location.setStoreProductId(request.storeProductId());
        location.setMapId(request.mapId());
        location.setSectorId(references.sector() == null ? null : references.sector().getId());
        location.setAisleId(references.aisle() == null ? null : references.aisle().getId());
        location.setShelfBlockId(references.shelfBlock() == null ? null : references.shelfBlock().getId());
        location.setSide(request.side());
        location.setModule(request.module());
        location.setShelfLevel(request.shelfLevel());
        location.setX(request.x());
        location.setY(request.y());
        location.setNavigationNodeId(references.node().getId());
        location.setPrimaryLocation(request.primaryLocation());
        location.setActive(request.active());
    }

    private void apply(ProductLocation location, UpdateProductLocationRequest request, ResolvedReferences references) {
        apply(location, new CreateProductLocationRequest(
                request.storeProductId(), request.mapId(), request.sectorId(), request.aisleId(), request.shelfBlockId(),
                request.side(), request.module(), request.shelfLevel(), request.x(), request.y(), request.navigationNodeId(),
                request.primaryLocation(), request.active()), references);
    }

    private Store ensureStore(UUID storeId) {
        return storeRepository.findById(storeId)
                .orElseThrow(() -> new StoreNotFoundException(storeId));
    }

    private ProductLocationAdminResponse toResponse(ProductLocation location) {
        return new ProductLocationAdminResponse(
                location.getId(),
                location.getStoreId(),
                location.getStoreProductId(),
                location.getMapId(),
                location.getSectorId(),
                location.getAisleId(),
                location.getShelfBlockId(),
                location.getSide(),
                location.getModule(),
                location.getShelfLevel(),
                location.getX(),
                location.getY(),
                location.getNavigationNodeId(),
                location.isPrimaryLocation(),
                location.isActive());
    }

    private record ResolvedReferences(Sector sector, Aisle aisle, ShelfBlock shelfBlock, MapNode node) {
    }
}
