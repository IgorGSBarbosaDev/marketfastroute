CREATE INDEX ix_store_active_name
    ON store (active, name);

CREATE INDEX ix_store_map_store_status
    ON store_map (store_id, status);

CREATE INDEX ix_sector_map_active
    ON sector (map_id, active);

CREATE INDEX ix_aisle_map_active
    ON aisle (map_id, active);

CREATE INDEX ix_shelf_block_map_active
    ON shelf_block (map_id, active);

CREATE INDEX ix_category_parent_active
    ON category (parent_id, active);

CREATE INDEX ix_product_category_active
    ON product (category_id, active);

CREATE INDEX ix_product_name_lower
    ON product (lower(name));

CREATE INDEX ix_store_product_store_active
    ON store_product (store_id, active, product_id);

CREATE INDEX ix_product_location_store_map_active
    ON product_location (store_id, map_id, active);

CREATE INDEX ix_product_location_store_product_active
    ON product_location (store_product_id, active);

CREATE INDEX ix_product_location_map_node
    ON product_location (map_id, navigation_node_id);

CREATE INDEX ix_point_of_interest_map_active_type
    ON point_of_interest (map_id, active, type);

CREATE INDEX ix_map_node_map_active_type
    ON map_node (map_id, active, type);

CREATE INDEX ix_map_edge_map_from_active
    ON map_edge (map_id, from_node_id, active);

CREATE INDEX ix_map_edge_map_to_active
    ON map_edge (map_id, to_node_id, active);
