CREATE TABLE product_location (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL,
    store_product_id UUID NOT NULL,
    map_id UUID NOT NULL,
    sector_id UUID,
    aisle_id UUID,
    shelf_block_id UUID,
    side VARCHAR(10),
    module VARCHAR,
    shelf_level INTEGER,
    x NUMERIC(12, 4),
    y NUMERIC(12, 4),
    navigation_node_id UUID NOT NULL,
    primary_location BOOLEAN NOT NULL DEFAULT FALSE,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT ck_product_location_side CHECK (side IS NULL OR side IN ('LEFT', 'RIGHT', 'CENTER')),
    CONSTRAINT ck_product_location_primary_active CHECK (NOT primary_location OR active),
    CONSTRAINT fk_product_location_store_product
        FOREIGN KEY (store_id, store_product_id)
        REFERENCES store_product (store_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_product_location_store_map
        FOREIGN KEY (store_id, map_id)
        REFERENCES store_map (store_id, id) ON DELETE CASCADE,
    CONSTRAINT fk_product_location_sector
        FOREIGN KEY (map_id, sector_id)
        REFERENCES sector (map_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_product_location_aisle
        FOREIGN KEY (map_id, aisle_id)
        REFERENCES aisle (map_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_product_location_shelf_block
        FOREIGN KEY (map_id, shelf_block_id)
        REFERENCES shelf_block (map_id, id) ON DELETE RESTRICT,
    CONSTRAINT fk_product_location_aisle_sector
        FOREIGN KEY (map_id, aisle_id, sector_id)
        REFERENCES aisle (map_id, id, sector_id) ON DELETE RESTRICT,
    CONSTRAINT fk_product_location_shelf_block_hierarchy
        FOREIGN KEY (map_id, shelf_block_id, aisle_id, sector_id)
        REFERENCES shelf_block (map_id, id, aisle_id, sector_id) ON DELETE RESTRICT,
    CONSTRAINT fk_product_location_navigation_node
        FOREIGN KEY (map_id, navigation_node_id)
        REFERENCES map_node (map_id, id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX ux_product_location_one_primary
    ON product_location (store_product_id, map_id)
    WHERE primary_location = TRUE;
