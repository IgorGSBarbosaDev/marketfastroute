package com.marketfastroute.map;

import com.marketfastroute.admin.AdminResourceNotFoundException;
import com.marketfastroute.admin.AdminValidationException;
import com.marketfastroute.admin.dto.CreateAisleRequest;
import com.marketfastroute.admin.dto.CreateShelfBlockRequest;
import com.marketfastroute.store.StoreMap;
import com.marketfastroute.store.StoreMapRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MapStructureAdminServiceTest {

    @Mock
    private StoreMapRepository storeMapRepository;

    @Mock
    private SectorRepository sectorRepository;

    @Mock
    private AisleRepository aisleRepository;

    @Mock
    private ShelfBlockRepository shelfBlockRepository;

    private MapStructureAdminService service;

    @BeforeEach
    void setUp() {
        service = new MapStructureAdminService(
                new MapAdminSupport(storeMapRepository), sectorRepository, aisleRepository, shelfBlockRepository);
    }

    @Test
    void rejectsAisleSectorFromAnotherMap() {
        UUID mapId = UUID.randomUUID();
        UUID foreignSectorId = UUID.randomUUID();
        StoreMap map = map(mapId);
        when(storeMapRepository.findById(mapId)).thenReturn(Optional.of(map));
        when(aisleRepository.existsByStoreMap_IdAndCode(mapId, "A-1")).thenReturn(false);
        when(sectorRepository.findByStoreMap_IdAndId(mapId, foreignSectorId)).thenReturn(Optional.empty());

        assertThrows(AdminResourceNotFoundException.class, () -> service.createAisle(
                mapId,
                new CreateAisleRequest(
                        foreignSectorId, "A-1", "Aisle", BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, true)));
    }

    @Test
    void rejectsShelfBlockWithIncompatibleSectorAndAisle() {
        UUID mapId = UUID.randomUUID();
        UUID sectorId = UUID.randomUUID();
        UUID otherSectorId = UUID.randomUUID();
        UUID aisleId = UUID.randomUUID();
        StoreMap map = map(mapId);
        Sector sector = new Sector();
        sector.setId(sectorId);
        sector.setStoreMap(map);
        Aisle aisle = new Aisle();
        aisle.setId(aisleId);
        aisle.setStoreMap(map);
        aisle.setSectorId(otherSectorId);

        when(storeMapRepository.findById(mapId)).thenReturn(Optional.of(map));
        when(shelfBlockRepository.existsByStoreMap_IdAndCode(mapId, "B-1")).thenReturn(false);
        when(sectorRepository.findByStoreMap_IdAndId(mapId, sectorId)).thenReturn(Optional.of(sector));
        when(aisleRepository.findByStoreMap_IdAndId(mapId, aisleId)).thenReturn(Optional.of(aisle));

        assertThrows(AdminValidationException.class, () -> service.createShelfBlock(
                mapId,
                new CreateShelfBlockRequest(
                        sectorId, aisleId, "B-1", "Block", BigDecimal.ZERO, BigDecimal.ZERO,
                        BigDecimal.TEN, BigDecimal.TEN, BigDecimal.ZERO, true)));
    }

    private StoreMap map(UUID id) {
        return mock(StoreMap.class);
    }
}
