package com.marketfastroute.map;

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
@Table(name = "point_of_interest")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PointOfInterest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "map_id", nullable = false)
    private StoreMap storeMap;

    @Column(name = "navigation_node_id")
    private UUID navigationNodeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "navigation_node_id", referencedColumnName = "id", insertable = false, updatable = false)
    private MapNode navigationNode;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 30)
    private PointOfInterestType type;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "x", nullable = false, precision = 12, scale = 4)
    private BigDecimal x;

    @Column(name = "y", nullable = false, precision = 12, scale = 4)
    private BigDecimal y;

    @Column(name = "active", nullable = false)
    private boolean active = true;
}
