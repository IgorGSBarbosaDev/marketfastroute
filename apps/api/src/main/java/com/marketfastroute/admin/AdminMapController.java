package com.marketfastroute.admin;

import com.marketfastroute.admin.dto.AisleAdminResponse;
import com.marketfastroute.admin.dto.CreateAisleRequest;
import com.marketfastroute.admin.dto.CreateMapEdgeRequest;
import com.marketfastroute.admin.dto.CreateMapNodeRequest;
import com.marketfastroute.admin.dto.CreatePointOfInterestRequest;
import com.marketfastroute.admin.dto.CreateSectorRequest;
import com.marketfastroute.admin.dto.CreateShelfBlockRequest;
import com.marketfastroute.admin.dto.MapEdgeAdminResponse;
import com.marketfastroute.admin.dto.MapNodeAdminResponse;
import com.marketfastroute.admin.dto.PointOfInterestAdminResponse;
import com.marketfastroute.admin.dto.SectorAdminResponse;
import com.marketfastroute.admin.dto.ShelfBlockAdminResponse;
import com.marketfastroute.admin.dto.UpdateAisleRequest;
import com.marketfastroute.admin.dto.UpdateMapEdgeRequest;
import com.marketfastroute.admin.dto.UpdateMapNodeRequest;
import com.marketfastroute.admin.dto.UpdatePointOfInterestRequest;
import com.marketfastroute.admin.dto.UpdateSectorRequest;
import com.marketfastroute.admin.dto.UpdateShelfBlockRequest;
import com.marketfastroute.map.MapGraphAdminService;
import com.marketfastroute.map.MapStructureAdminService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/maps/{mapId}")
public class AdminMapController {

    private final MapStructureAdminService structureAdminService;
    private final MapGraphAdminService graphAdminService;

    public AdminMapController(
            MapStructureAdminService structureAdminService,
            MapGraphAdminService graphAdminService
    ) {
        this.structureAdminService = structureAdminService;
        this.graphAdminService = graphAdminService;
    }

    @GetMapping("/sectors")
    public List<SectorAdminResponse> findSectors(@PathVariable UUID mapId) {
        return structureAdminService.findSectors(mapId);
    }

    @GetMapping("/sectors/{sectorId}")
    public SectorAdminResponse findSector(@PathVariable UUID mapId, @PathVariable UUID sectorId) {
        return structureAdminService.findSector(mapId, sectorId);
    }

    @PostMapping("/sectors")
    public ResponseEntity<SectorAdminResponse> createSector(
            @PathVariable UUID mapId,
            @Valid @RequestBody CreateSectorRequest request
    ) {
        SectorAdminResponse response = structureAdminService.createSector(mapId, request);
        return ResponseEntity.created(URI.create("/api/v1/admin/maps/" + mapId + "/sectors/" + response.id()))
                .body(response);
    }

    @PutMapping("/sectors/{sectorId}")
    public SectorAdminResponse updateSector(
            @PathVariable UUID mapId,
            @PathVariable UUID sectorId,
            @Valid @RequestBody UpdateSectorRequest request
    ) {
        return structureAdminService.updateSector(mapId, sectorId, request);
    }

    @GetMapping("/aisles")
    public List<AisleAdminResponse> findAisles(@PathVariable UUID mapId) {
        return structureAdminService.findAisles(mapId);
    }

    @GetMapping("/aisles/{aisleId}")
    public AisleAdminResponse findAisle(@PathVariable UUID mapId, @PathVariable UUID aisleId) {
        return structureAdminService.findAisle(mapId, aisleId);
    }

    @PostMapping("/aisles")
    public ResponseEntity<AisleAdminResponse> createAisle(
            @PathVariable UUID mapId,
            @Valid @RequestBody CreateAisleRequest request
    ) {
        AisleAdminResponse response = structureAdminService.createAisle(mapId, request);
        return ResponseEntity.created(URI.create("/api/v1/admin/maps/" + mapId + "/aisles/" + response.id()))
                .body(response);
    }

    @PutMapping("/aisles/{aisleId}")
    public AisleAdminResponse updateAisle(
            @PathVariable UUID mapId,
            @PathVariable UUID aisleId,
            @Valid @RequestBody UpdateAisleRequest request
    ) {
        return structureAdminService.updateAisle(mapId, aisleId, request);
    }

    @GetMapping("/shelf-blocks")
    public List<ShelfBlockAdminResponse> findShelfBlocks(@PathVariable UUID mapId) {
        return structureAdminService.findShelfBlocks(mapId);
    }

    @GetMapping("/shelf-blocks/{shelfBlockId}")
    public ShelfBlockAdminResponse findShelfBlock(@PathVariable UUID mapId, @PathVariable UUID shelfBlockId) {
        return structureAdminService.findShelfBlock(mapId, shelfBlockId);
    }

    @PostMapping("/shelf-blocks")
    public ResponseEntity<ShelfBlockAdminResponse> createShelfBlock(
            @PathVariable UUID mapId,
            @Valid @RequestBody CreateShelfBlockRequest request
    ) {
        ShelfBlockAdminResponse response = structureAdminService.createShelfBlock(mapId, request);
        return ResponseEntity.created(URI.create("/api/v1/admin/maps/" + mapId + "/shelf-blocks/" + response.id()))
                .body(response);
    }

    @PutMapping("/shelf-blocks/{shelfBlockId}")
    public ShelfBlockAdminResponse updateShelfBlock(
            @PathVariable UUID mapId,
            @PathVariable UUID shelfBlockId,
            @Valid @RequestBody UpdateShelfBlockRequest request
    ) {
        return structureAdminService.updateShelfBlock(mapId, shelfBlockId, request);
    }

    @GetMapping("/points-of-interest")
    public List<PointOfInterestAdminResponse> findPointsOfInterest(@PathVariable UUID mapId) {
        return graphAdminService.findPointsOfInterest(mapId);
    }

    @GetMapping("/points-of-interest/{pointOfInterestId}")
    public PointOfInterestAdminResponse findPointOfInterest(
            @PathVariable UUID mapId,
            @PathVariable UUID pointOfInterestId
    ) {
        return graphAdminService.findPointOfInterest(mapId, pointOfInterestId);
    }

    @PostMapping("/points-of-interest")
    public ResponseEntity<PointOfInterestAdminResponse> createPointOfInterest(
            @PathVariable UUID mapId,
            @Valid @RequestBody CreatePointOfInterestRequest request
    ) {
        PointOfInterestAdminResponse response = graphAdminService.createPointOfInterest(mapId, request);
        return ResponseEntity.created(URI.create(
                "/api/v1/admin/maps/" + mapId + "/points-of-interest/" + response.id())).body(response);
    }

    @PutMapping("/points-of-interest/{pointOfInterestId}")
    public PointOfInterestAdminResponse updatePointOfInterest(
            @PathVariable UUID mapId,
            @PathVariable UUID pointOfInterestId,
            @Valid @RequestBody UpdatePointOfInterestRequest request
    ) {
        return graphAdminService.updatePointOfInterest(mapId, pointOfInterestId, request);
    }

    @GetMapping("/nodes")
    public List<MapNodeAdminResponse> findNodes(@PathVariable UUID mapId) {
        return graphAdminService.findNodes(mapId);
    }

    @GetMapping("/nodes/{nodeId}")
    public MapNodeAdminResponse findNode(@PathVariable UUID mapId, @PathVariable UUID nodeId) {
        return graphAdminService.findNode(mapId, nodeId);
    }

    @PostMapping("/nodes")
    public ResponseEntity<MapNodeAdminResponse> createNode(
            @PathVariable UUID mapId,
            @Valid @RequestBody CreateMapNodeRequest request
    ) {
        MapNodeAdminResponse response = graphAdminService.createNode(mapId, request);
        return ResponseEntity.created(URI.create("/api/v1/admin/maps/" + mapId + "/nodes/" + response.id()))
                .body(response);
    }

    @PutMapping("/nodes/{nodeId}")
    public MapNodeAdminResponse updateNode(
            @PathVariable UUID mapId,
            @PathVariable UUID nodeId,
            @Valid @RequestBody UpdateMapNodeRequest request
    ) {
        return graphAdminService.updateNode(mapId, nodeId, request);
    }

    @GetMapping("/edges")
    public List<MapEdgeAdminResponse> findEdges(@PathVariable UUID mapId) {
        return graphAdminService.findEdges(mapId);
    }

    @GetMapping("/edges/{edgeId}")
    public MapEdgeAdminResponse findEdge(@PathVariable UUID mapId, @PathVariable UUID edgeId) {
        return graphAdminService.findEdge(mapId, edgeId);
    }

    @PostMapping("/edges")
    public ResponseEntity<MapEdgeAdminResponse> createEdge(
            @PathVariable UUID mapId,
            @Valid @RequestBody CreateMapEdgeRequest request
    ) {
        MapEdgeAdminResponse response = graphAdminService.createEdge(mapId, request);
        return ResponseEntity.created(URI.create("/api/v1/admin/maps/" + mapId + "/edges/" + response.id()))
                .body(response);
    }

    @PutMapping("/edges/{edgeId}")
    public MapEdgeAdminResponse updateEdge(
            @PathVariable UUID mapId,
            @PathVariable UUID edgeId,
            @Valid @RequestBody UpdateMapEdgeRequest request
    ) {
        return graphAdminService.updateEdge(mapId, edgeId, request);
    }
}
