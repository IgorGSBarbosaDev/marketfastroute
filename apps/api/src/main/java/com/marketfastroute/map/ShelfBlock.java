package com.marketfastroute.map;

import com.marketfastroute.product.ProductLocation;
import com.marketfastroute.store.StoreMap;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "shelf_block",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_shelf_block_map_code", columnNames = {"map_id", "code"}),
                @UniqueConstraint(name = "uq_shelf_block_map_id", columnNames = {"map_id", "id"}),
                @UniqueConstraint(
                        name = "uq_shelf_block_map_id_hierarchy",
                        columnNames = {"map_id", "id", "aisle_id", "sector_id"})
        })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShelfBlock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "map_id", nullable = false)
    private StoreMap storeMap;

    @Column(name = "sector_id")
    private UUID sectorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sector_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Sector sector;

    @Column(name = "aisle_id")
    private UUID aisleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "aisle_id", referencedColumnName = "id", insertable = false, updatable = false)
    private Aisle aisle;

    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name")
    private String name;

    @Column(name = "x", nullable = false, precision = 12, scale = 4)
    private BigDecimal x;

    @Column(name = "y", nullable = false, precision = 12, scale = 4)
    private BigDecimal y;

    @Column(name = "width", nullable = false, precision = 12, scale = 4)
    private BigDecimal width;

    @Column(name = "height", nullable = false, precision = 12, scale = 4)
    private BigDecimal height;

    @Column(name = "rotation", nullable = false, precision = 12, scale = 4)
    private BigDecimal rotation;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "shelfBlock", fetch = FetchType.LAZY)
    private Set<ProductLocation> productLocations = new HashSet<>();
}
