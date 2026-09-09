package com.marketfastroute.map;

import com.marketfastroute.product.ProductLocation;
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
        name = "map_node",
        uniqueConstraints = @UniqueConstraint(name = "uq_map_node_map_id", columnNames = {"map_id", "id"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MapNode {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "map_id", nullable = false)
    private StoreMap storeMap;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private MapNodeType type;

    @Column(name = "x", nullable = false, precision = 12, scale = 4)
    private BigDecimal x;

    @Column(name = "y", nullable = false, precision = 12, scale = 4)
    private BigDecimal y;

    @Column(name = "label")
    private String label;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @OneToMany(mappedBy = "fromNode", fetch = FetchType.LAZY)
    private Set<MapEdge> outgoingEdges = new HashSet<>();

    @OneToMany(mappedBy = "toNode", fetch = FetchType.LAZY)
    private Set<MapEdge> incomingEdges = new HashSet<>();

    @OneToMany(mappedBy = "navigationNode", fetch = FetchType.LAZY)
    private Set<PointOfInterest> pointsOfInterest = new HashSet<>();

    @OneToMany(mappedBy = "navigationNode", fetch = FetchType.LAZY)
    private Set<ProductLocation> productLocations = new HashSet<>();
}
