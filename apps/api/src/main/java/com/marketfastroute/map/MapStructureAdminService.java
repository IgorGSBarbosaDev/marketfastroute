package com.marketfastroute.map;

import com.marketfastroute.admin.AdminConflictException;
import com.marketfastroute.admin.AdminValidationException;
import com.marketfastroute.admin.dto.AisleAdminResponse;
import com.marketfastroute.admin.dto.CreateAisleRequest;
import com.marketfastroute.admin.dto.CreateSectorRequest;
import com.marketfastroute.admin.dto.CreateShelfBlockRequest;
import com.marketfastroute.admin.dto.SectorAdminResponse;
import com.marketfastroute.admin.dto.ShelfBlockAdminResponse;
import com.marketfastroute.admin.dto.UpdateAisleRequest;
import com.marketfastroute.admin.dto.UpdateSectorRequest;
import com.marketfastroute.admin.dto.UpdateShelfBlockRequest;
import com.marketfastroute.store.StoreMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class MapStructureAdminService {

    private final MapAdminSupport support;
    private final SectorRepository sectorRepository;
    private final AisleRepository aisleRepository;
    private final ShelfBlockRepository shelfBlockRepository;

    public MapStructureAdminService(
            MapAdminSupport support,
            SectorRepository sectorRepository,
            AisleRepository aisleRepository,
            ShelfBlockRepository shelfBlockRepository
    ) {
        this.support = support;
        this.sectorRepository = sectorRepository;
        this.aisleRepository = aisleRepository;
        this.shelfBlockRepository = shelfBlockRepository;
    }

    public List<SectorAdminResponse> findSectors(UUID mapId) {
        support.findMap(mapId);
        return sectorRepository.findByStoreMap_IdOrderById(mapId).stream().map(this::toResponse).toList();
    }

    public SectorAdminResponse findSector(UUID mapId, UUID sectorId) {
        return toResponse(support.findSector(mapId, sectorId, sectorRepository));
    }

    @Transactional
    public SectorAdminResponse createSector(UUID mapId, CreateSectorRequest request) {
        StoreMap map = support.findMap(mapId);
        ensureSectorCode(mapId, request.code(), null);
        Sector sector = new Sector();
        sector.setStoreMap(map);
        apply(sector, request);
        return toResponse(sectorRepository.save(sector));
    }

    @Transactional
    public SectorAdminResponse updateSector(UUID mapId, UUID sectorId, UpdateSectorRequest request) {
        Sector sector = support.findSector(mapId, sectorId, sectorRepository);
        ensureSectorCode(mapId, request.code(), sectorId);
        sector.setName(request.name());
        sector.setCode(request.code());
        sector.setX(request.x());
        sector.setY(request.y());
        sector.setWidth(request.width());
        sector.setHeight(request.height());
        sector.setRotation(request.rotation());
        sector.setActive(request.active());
        return toResponse(sectorRepository.save(sector));
    }

    public List<AisleAdminResponse> findAisles(UUID mapId) {
        support.findMap(mapId);
        return aisleRepository.findByStoreMap_IdOrderById(mapId).stream().map(this::toResponse).toList();
    }

    public AisleAdminResponse findAisle(UUID mapId, UUID aisleId) {
        return toResponse(support.findAisle(mapId, aisleId, aisleRepository));
    }

    @Transactional
    public AisleAdminResponse createAisle(UUID mapId, CreateAisleRequest request) {
        StoreMap map = support.findMap(mapId);
        ensureAisleCode(mapId, request.code(), null);
        Sector sector = request.sectorId() == null ? null : support.findSector(mapId, request.sectorId(), sectorRepository);

        Aisle aisle = new Aisle();
        aisle.setStoreMap(map);
        aisle.setSectorId(sector == null ? null : sector.getId());
        apply(aisle, request);
        return toResponse(aisleRepository.save(aisle));
    }

    @Transactional
    public AisleAdminResponse updateAisle(UUID mapId, UUID aisleId, UpdateAisleRequest request) {
        Aisle aisle = support.findAisle(mapId, aisleId, aisleRepository);
        ensureAisleCode(mapId, request.code(), aisleId);
        Sector sector = request.sectorId() == null ? null : support.findSector(mapId, request.sectorId(), sectorRepository);

        aisle.setSectorId(sector == null ? null : sector.getId());
        aisle.setCode(request.code());
        aisle.setName(request.name());
        aisle.setX(request.x());
        aisle.setY(request.y());
        aisle.setWidth(request.width());
        aisle.setHeight(request.height());
        aisle.setRotation(request.rotation());
        aisle.setActive(request.active());
        return toResponse(aisleRepository.save(aisle));
    }

    public List<ShelfBlockAdminResponse> findShelfBlocks(UUID mapId) {
        support.findMap(mapId);
        return shelfBlockRepository.findByStoreMap_IdOrderById(mapId).stream().map(this::toResponse).toList();
    }

    public ShelfBlockAdminResponse findShelfBlock(UUID mapId, UUID shelfBlockId) {
        return toResponse(support.findShelfBlock(mapId, shelfBlockId, shelfBlockRepository));
    }

    @Transactional
    public ShelfBlockAdminResponse createShelfBlock(UUID mapId, CreateShelfBlockRequest request) {
        StoreMap map = support.findMap(mapId);
        ensureShelfBlockCode(mapId, request.code(), null);
        Hierarchy hierarchy = resolveHierarchy(mapId, request.sectorId(), request.aisleId());

        ShelfBlock shelfBlock = new ShelfBlock();
        shelfBlock.setStoreMap(map);
        shelfBlock.setSectorId(hierarchy.sectorId());
        shelfBlock.setAisleId(hierarchy.aisleId());
        apply(shelfBlock, request);
        return toResponse(shelfBlockRepository.save(shelfBlock));
    }

    @Transactional
    public ShelfBlockAdminResponse updateShelfBlock(
            UUID mapId,
            UUID shelfBlockId,
            UpdateShelfBlockRequest request
    ) {
        ShelfBlock shelfBlock = support.findShelfBlock(mapId, shelfBlockId, shelfBlockRepository);
        ensureShelfBlockCode(mapId, request.code(), shelfBlockId);
        Hierarchy hierarchy = resolveHierarchy(mapId, request.sectorId(), request.aisleId());

        shelfBlock.setSectorId(hierarchy.sectorId());
        shelfBlock.setAisleId(hierarchy.aisleId());
        shelfBlock.setCode(request.code());
        shelfBlock.setName(request.name());
        shelfBlock.setX(request.x());
        shelfBlock.setY(request.y());
        shelfBlock.setWidth(request.width());
        shelfBlock.setHeight(request.height());
        shelfBlock.setRotation(request.rotation());
        shelfBlock.setActive(request.active());
        return toResponse(shelfBlockRepository.save(shelfBlock));
    }

    private Hierarchy resolveHierarchy(UUID mapId, UUID sectorId, UUID aisleId) {
        Sector sector = sectorId == null ? null : support.findSector(mapId, sectorId, sectorRepository);
        Aisle aisle = aisleId == null ? null : support.findAisle(mapId, aisleId, aisleRepository);
        if (sector != null && aisle != null && !Objects.equals(aisle.getSectorId(), sector.getId())) {
            throw new AdminValidationException("Aisle does not belong to the selected sector");
        }
        return new Hierarchy(
                sector == null ? null : sector.getId(),
                aisle == null ? null : aisle.getId()
        );
    }

    private void ensureSectorCode(UUID mapId, String code, UUID sectorId) {
        boolean exists = sectorId == null
                ? sectorRepository.existsByStoreMap_IdAndCode(mapId, code)
                : sectorRepository.existsByStoreMap_IdAndCodeAndIdNot(mapId, code, sectorId);
        if (exists) {
            throw new AdminConflictException("Sector code is already in use in the map");
        }
    }

    private void ensureAisleCode(UUID mapId, String code, UUID aisleId) {
        boolean exists = aisleId == null
                ? aisleRepository.existsByStoreMap_IdAndCode(mapId, code)
                : aisleRepository.existsByStoreMap_IdAndCodeAndIdNot(mapId, code, aisleId);
        if (exists) {
            throw new AdminConflictException("Aisle code is already in use in the map");
        }
    }

    private void ensureShelfBlockCode(UUID mapId, String code, UUID shelfBlockId) {
        boolean exists = shelfBlockId == null
                ? shelfBlockRepository.existsByStoreMap_IdAndCode(mapId, code)
                : shelfBlockRepository.existsByStoreMap_IdAndCodeAndIdNot(mapId, code, shelfBlockId);
        if (exists) {
            throw new AdminConflictException("Shelf block code is already in use in the map");
        }
    }

    private void apply(Sector sector, CreateSectorRequest request) {
        sector.setName(request.name());
        sector.setCode(request.code());
        sector.setX(request.x());
        sector.setY(request.y());
        sector.setWidth(request.width());
        sector.setHeight(request.height());
        sector.setRotation(request.rotation());
        sector.setActive(request.active());
    }

    private void apply(Aisle aisle, CreateAisleRequest request) {
        aisle.setCode(request.code());
        aisle.setName(request.name());
        aisle.setX(request.x());
        aisle.setY(request.y());
        aisle.setWidth(request.width());
        aisle.setHeight(request.height());
        aisle.setRotation(request.rotation());
        aisle.setActive(request.active());
    }

    private void apply(ShelfBlock shelfBlock, CreateShelfBlockRequest request) {
        shelfBlock.setCode(request.code());
        shelfBlock.setName(request.name());
        shelfBlock.setX(request.x());
        shelfBlock.setY(request.y());
        shelfBlock.setWidth(request.width());
        shelfBlock.setHeight(request.height());
        shelfBlock.setRotation(request.rotation());
        shelfBlock.setActive(request.active());
    }

    private SectorAdminResponse toResponse(Sector sector) {
        return new SectorAdminResponse(
                sector.getId(), sector.getStoreMap().getId(), sector.getName(), sector.getCode(),
                sector.getX(), sector.getY(), sector.getWidth(), sector.getHeight(), sector.getRotation(),
                sector.isActive());
    }

    private AisleAdminResponse toResponse(Aisle aisle) {
        return new AisleAdminResponse(
                aisle.getId(), aisle.getStoreMap().getId(), aisle.getSectorId(), aisle.getCode(), aisle.getName(),
                aisle.getX(), aisle.getY(), aisle.getWidth(), aisle.getHeight(), aisle.getRotation(), aisle.isActive());
    }

    private ShelfBlockAdminResponse toResponse(ShelfBlock shelfBlock) {
        return new ShelfBlockAdminResponse(
                shelfBlock.getId(), shelfBlock.getStoreMap().getId(), shelfBlock.getSectorId(), shelfBlock.getAisleId(),
                shelfBlock.getCode(), shelfBlock.getName(), shelfBlock.getX(), shelfBlock.getY(),
                shelfBlock.getWidth(), shelfBlock.getHeight(), shelfBlock.getRotation(), shelfBlock.isActive());
    }

    private record Hierarchy(UUID sectorId, UUID aisleId) {
    }
}
