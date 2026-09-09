package com.marketfastroute.map;

import com.marketfastroute.store.StoreMap;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(
        name = "map_edge",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_map_edge_map_from_to",
                columnNames = {"map_id", "from_node_id", "to_node_id"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MapEdge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "map_id", nullable = false)
    private StoreMap storeMap;

    @Column(name = "from_node_id", nullable = false)
    private UUID fromNodeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_node_id", referencedColumnName = "id", insertable = false, updatable = false)
    private MapNode fromNode;

    @Column(name = "to_node_id", nullable = false)
    private UUID toNodeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "to_node_id", referencedColumnName = "id", insertable = false, updatable = false)
    private MapNode toNode;

    @Column(name = "distance_meters", nullable = false, precision = 12, scale = 4)
    private BigDecimal distanceMeters;

    @Column(name = "bidirectional", nullable = false)
    private boolean bidirectional;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
