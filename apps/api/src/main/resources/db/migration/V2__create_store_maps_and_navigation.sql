CREATE TABLE store_map (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL,
    version INTEGER NOT NULL,
    name VARCHAR NOT NULL,
    width NUMERIC(12, 4) NOT NULL,
    height NUMERIC(12, 4) NOT NULL,
    scale_meters_per_unit NUMERIC(12, 6) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_store_map_store_version UNIQUE (store_id, version),
    CONSTRAINT uq_store_map_store_id UNIQUE (store_id, id),
    CONSTRAINT ck_store_map_version_positive CHECK (version > 0),
    CONSTRAINT ck_store_map_width_positive CHECK (width > 0),
    CONSTRAINT ck_store_map_height_positive CHECK (height > 0),
    CONSTRAINT ck_store_map_scale_positive CHECK (scale_meters_per_unit > 0),
    CONSTRAINT ck_store_map_status CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED')),
    CONSTRAINT fk_store_map_store
        FOREIGN KEY (store_id) REFERENCES store (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX ux_store_map_one_active_per_store
    ON store_map (store_id)
    WHERE status = 'ACTIVE';

CREATE TABLE sector (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    map_id UUID NOT NULL,
    name VARCHAR NOT NULL,
    code VARCHAR NOT NULL,
    x NUMERIC(12, 4) NOT NULL,
    y NUMERIC(12, 4) NOT NULL,
    width NUMERIC(12, 4) NOT NULL,
    height NUMERIC(12, 4) NOT NULL,
    rotation NUMERIC(12, 4) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_sector_map_code UNIQUE (map_id, code),
    CONSTRAINT uq_sector_map_id UNIQUE (map_id, id),
    CONSTRAINT ck_sector_width_positive CHECK (width > 0),
    CONSTRAINT ck_sector_height_positive CHECK (height > 0),
    CONSTRAINT fk_sector_map
        FOREIGN KEY (map_id) REFERENCES store_map (id) ON DELETE CASCADE
);

CREATE TABLE aisle (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    map_id UUID NOT NULL,
    sector_id UUID,
    code VARCHAR NOT NULL,
    name VARCHAR NOT NULL,
    x NUMERIC(12, 4) NOT NULL,
    y NUMERIC(12, 4) NOT NULL,
    width NUMERIC(12, 4) NOT NULL,
    height NUMERIC(12, 4) NOT NULL,
    rotation NUMERIC(12, 4) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_aisle_map_code UNIQUE (map_id, code),
    CONSTRAINT uq_aisle_map_id UNIQUE (map_id, id),
    CONSTRAINT uq_aisle_map_id_sector UNIQUE (map_id, id, sector_id),
    CONSTRAINT ck_aisle_width_positive CHECK (width > 0),
    CONSTRAINT ck_aisle_height_positive CHECK (height > 0),
    CONSTRAINT fk_aisle_map
        FOREIGN KEY (map_id) REFERENCES store_map (id) ON DELETE CASCADE,
    CONSTRAINT fk_aisle_sector
        FOREIGN KEY (map_id, sector_id) REFERENCES sector (map_id, id) ON DELETE RESTRICT
);

CREATE TABLE shelf_block (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    map_id UUID NOT NULL,
    sector_id UUID,
    aisle_id UUID,
    code VARCHAR NOT NULL,
    name VARCHAR,
    x NUMERIC(12, 4) NOT NULL,
    y NUMERIC(12, 4) NOT NULL,
    width NUMERIC(12, 4) NOT NULL,
    height NUMERIC(12, 4) NOT NULL,
    rotation NUMERIC(12, 4) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_shelf_block_map_code UNIQUE (map_id, code),
    CONSTRAINT uq_shelf_block_map_id UNIQUE (map_id, id),
    CONSTRAINT uq_shelf_block_map_id_hierarchy UNIQUE (map_id, id, aisle_id, sector_id),
    CONSTRAINT ck_shelf_block_width_positive CHECK (width > 0),
    CONSTRAINT ck_shelf_block_height_positive CHECK (height > 0),
    CONSTRAINT fk_shelf_block_map
        FOREIGN KEY (map_id) REFERENCES store_map (id) ON DELETE CASCADE,
    CONSTRAINT fk_shelf_block_sector
        FOREIGN KEY (map_id, sector_id) REFERENCES sector (map_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_shelf_block_aisle
        FOREIGN KEY (map_id, aisle_id) REFERENCES aisle (map_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_shelf_block_aisle_sector
        FOREIGN KEY (map_id, aisle_id, sector_id)
        REFERENCES aisle (map_id, id, sector_id) ON DELETE RESTRICT
);

CREATE TABLE map_node (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    map_id UUID NOT NULL,
    type VARCHAR(30) NOT NULL,
    x NUMERIC(12, 4) NOT NULL,
    y NUMERIC(12, 4) NOT NULL,
    label VARCHAR,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_map_node_map_id UNIQUE (map_id, id),
    CONSTRAINT ck_map_node_type CHECK (
        type IN ('PATH', 'INTERSECTION', 'ENTRANCE', 'EXIT', 'PRODUCT_ACCESS', 'CHECKOUT')
    ),
    CONSTRAINT fk_map_node_map
        FOREIGN KEY (map_id) REFERENCES store_map (id) ON DELETE CASCADE
);

CREATE TABLE point_of_interest (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    map_id UUID NOT NULL,
    navigation_node_id UUID,
    type VARCHAR(30) NOT NULL,
    name VARCHAR NOT NULL,
    x NUMERIC(12, 4) NOT NULL,
    y NUMERIC(12, 4) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_point_of_interest_type CHECK (
        type IN (
            'ENTRANCE', 'EXIT', 'CHECKOUT', 'CART', 'RESTROOM',
            'CUSTOMER_SERVICE', 'PARKING', 'ELEVATOR', 'STAIRS', 'OTHER'
        )
    ),
    CONSTRAINT fk_point_of_interest_map
        FOREIGN KEY (map_id) REFERENCES store_map (id) ON DELETE CASCADE,
    CONSTRAINT fk_point_of_interest_navigation_node
        FOREIGN KEY (map_id, navigation_node_id)
        REFERENCES map_node (map_id, id) ON DELETE RESTRICT
);

CREATE TABLE map_edge (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    map_id UUID NOT NULL,
    from_node_id UUID NOT NULL,
    to_node_id UUID NOT NULL,
    distance_meters NUMERIC(12, 4) NOT NULL,
    bidirectional BOOLEAN NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_map_edge_map_from_to UNIQUE (map_id, from_node_id, to_node_id),
    CONSTRAINT ck_map_edge_nodes_distinct CHECK (from_node_id <> to_node_id),
    CONSTRAINT ck_map_edge_distance_positive CHECK (distance_meters > 0),
    CONSTRAINT fk_map_edge_map
        FOREIGN KEY (map_id) REFERENCES store_map (id) ON DELETE CASCADE,
    CONSTRAINT fk_map_edge_from_node
        FOREIGN KEY (map_id, from_node_id)
        REFERENCES map_node (map_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_map_edge_to_node
        FOREIGN KEY (map_id, to_node_id)
        REFERENCES map_node (map_id, id) ON DELETE CASCADE
);
