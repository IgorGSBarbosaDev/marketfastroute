package com.marketfastroute.store;

import com.marketfastroute.map.Aisle;
import com.marketfastroute.map.MapEdge;
import com.marketfastroute.map.MapNode;
import com.marketfastroute.map.MapStatus;
import com.marketfastroute.map.PointOfInterest;
import com.marketfastroute.map.Sector;
import com.marketfastroute.map.ShelfBlock;
import com.marketfastroute.product.ProductLocation;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "store_map",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_store_map_store_version", columnNames = {"store_id", "version"}),
                @UniqueConstraint(name = "uq_store_map_store_id", columnNames = {"store_id", "id"})
        })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StoreMap {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(name = "version", nullable = false)
    private int version;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "width", nullable = false, precision = 12, scale = 4)
    private BigDecimal width;

    @Column(name = "height", nullable = false, precision = 12, scale = 4)
    private BigDecimal height;

    @Column(name = "scale_meters_per_unit", nullable = false, precision = 12, scale = 6)
    private BigDecimal scaleMetersPerUnit;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private MapStatus status = MapStatus.DRAFT;

    @Column(name = "created_at", nullable = false, updatable = false)
    @CreationTimestamp
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    @UpdateTimestamp
    private OffsetDateTime updatedAt;

    @OneToMany(mappedBy = "storeMap", fetch = FetchType.LAZY)
    private Set<Sector> sectors = new HashSet<>();

    @OneToMany(mappedBy = "storeMap", fetch = FetchType.LAZY)
    private Set<Aisle> aisles = new HashSet<>();

    @OneToMany(mappedBy = "storeMap", fetch = FetchType.LAZY)
    private Set<ShelfBlock> shelfBlocks = new HashSet<>();

    @OneToMany(mappedBy = "storeMap", fetch = FetchType.LAZY)
    private Set<PointOfInterest> pointsOfInterest = new HashSet<>();

    @OneToMany(mappedBy = "storeMap", fetch = FetchType.LAZY)
    private Set<MapNode> nodes = new HashSet<>();

    @OneToMany(mappedBy = "storeMap", fetch = FetchType.LAZY)
    private Set<MapEdge> edges = new HashSet<>();

    @OneToMany(mappedBy = "storeMap", fetch = FetchType.LAZY)
    private Set<ProductLocation> productLocations = new HashSet<>();
}
