package com.marketfastroute.product;

import com.marketfastroute.map.Aisle;
import com.marketfastroute.map.MapNode;
import com.marketfastroute.map.Sector;
import com.marketfastroute.map.ShelfBlock;
import com.marketfastroute.store.StoreMap;
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
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "product_location")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    /**
     * Technical discriminator shared by the composite foreign keys.
     * Associations below are read-only views of these columns.
     */
    @Column(name = "store_id", nullable = false)
    private UUID storeId;

    @Column(name = "store_product_id", nullable = false)
    private UUID storeProductId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_product_id", referencedColumnName = "id", insertable = false, updatable = false)
    private StoreProduct storeProduct;

    @Column(name = "map_id", nullable = false)
    private UUID mapId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "map_id", referencedColumnName = "id", insertable = false, updatable = false)
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

    @Column(name = "shelf_block_id")
    private UUID shelfBlockId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "shelf_block_id", referencedColumnName = "id", insertable = false, updatable = false)
    private ShelfBlock shelfBlock;

    @Enumerated(EnumType.STRING)
    @Column(name = "side", length = 10)
    private ProductLocationSide side;

    @Column(name = "module")
    private String module;

    @Column(name = "shelf_level")
    private Integer shelfLevel;

    @Column(name = "x", precision = 12, scale = 4)
    private BigDecimal x;

    @Column(name = "y", precision = 12, scale = 4)
    private BigDecimal y;

    @Column(name = "navigation_node_id", nullable = false)
    private UUID navigationNodeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "navigation_node_id", referencedColumnName = "id", insertable = false, updatable = false)
    private MapNode navigationNode;

    @Column(name = "primary_location", nullable = false)
    private boolean primaryLocation = false;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
