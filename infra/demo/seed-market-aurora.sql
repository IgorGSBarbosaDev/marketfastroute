BEGIN;

DO $$
DECLARE
    demo_store_id UUID := md5('marketfastroute.demo.store')::uuid;
    demo_map_id UUID := md5('marketfastroute.demo.map')::uuid;
BEGIN
    IF EXISTS (
        SELECT 1
        FROM store
        WHERE (code = 'MFR-DEMO-AURORA' AND id <> demo_store_id)
           OR (id = demo_store_id AND code <> 'MFR-DEMO-AURORA')
    ) THEN
        RAISE EXCEPTION 'Refusing to use a store code or ID owned by another record';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM store_map
        WHERE (store_id = demo_store_id AND version = 1
               AND (id <> demo_map_id OR status <> 'DRAFT'))
           OR (id = demo_map_id AND (store_id <> demo_store_id OR version <> 1))
    ) THEN
        RAISE EXCEPTION 'Refusing to refresh Mercado Aurora: map identity changed or version 1 is not a draft';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM map_node node
        CROSS JOIN generate_series(0, 3) AS row_axis(row_index)
        CROSS JOIN generate_series(0, 7) AS column_axis(col_index)
        WHERE node.id = md5('marketfastroute.demo.node.' || row_index || '.' || col_index)::uuid
          AND node.map_id <> demo_map_id
    ) THEN
        RAISE EXCEPTION 'Refusing to overwrite a navigation node outside the demo map';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM point_of_interest point
        CROSS JOIN (VALUES ('ENTRANCE'), ('CHECKOUT'), ('CART')) AS demo(code)
        WHERE point.id = md5('marketfastroute.demo.poi.' || demo.code)::uuid
          AND point.map_id <> demo_map_id
    ) THEN
        RAISE EXCEPTION 'Refusing to overwrite a point of interest outside the demo map';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM product_location location
        JOIN (VALUES
            ('MFR-DEMO-BANANA'), ('MFR-DEMO-MACA'), ('MFR-DEMO-PAO'), ('MFR-DEMO-BOLO'),
            ('MFR-DEMO-AGUA'), ('MFR-DEMO-SUCO'), ('MFR-DEMO-LEITE'), ('MFR-DEMO-QUEIJO'),
            ('MFR-DEMO-ARROZ'), ('MFR-DEMO-FEIJAO'), ('MFR-DEMO-DETERGENTE'), ('MFR-DEMO-SABAO')
        ) AS demo(sku) ON location.id = md5('marketfastroute.demo.location.' || demo.sku)::uuid
        WHERE location.store_id <> demo_store_id OR location.map_id <> demo_map_id
    ) THEN
        RAISE EXCEPTION 'Refusing to overwrite a product location outside the demo store and map';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM category category
        JOIN (VALUES
            ('MFR-DEMO-HORTIFRUTI', md5('marketfastroute.demo.category.MFR-DEMO-HORTIFRUTI')::uuid),
            ('MFR-DEMO-PADARIA', md5('marketfastroute.demo.category.MFR-DEMO-PADARIA')::uuid),
            ('MFR-DEMO-BEBIDAS', md5('marketfastroute.demo.category.MFR-DEMO-BEBIDAS')::uuid),
            ('MFR-DEMO-LATICINIOS', md5('marketfastroute.demo.category.MFR-DEMO-LATICINIOS')::uuid),
            ('MFR-DEMO-MERCEARIA', md5('marketfastroute.demo.category.MFR-DEMO-MERCEARIA')::uuid),
            ('MFR-DEMO-LIMPEZA', md5('marketfastroute.demo.category.MFR-DEMO-LIMPEZA')::uuid)
        ) AS expected(code, id) ON category.code = expected.code
        WHERE category.id <> expected.id
    ) OR EXISTS (
        SELECT 1 FROM category WHERE code LIKE 'MFR-DEMO-%'
          AND code NOT IN (
            'MFR-DEMO-HORTIFRUTI', 'MFR-DEMO-PADARIA', 'MFR-DEMO-BEBIDAS',
            'MFR-DEMO-LATICINIOS', 'MFR-DEMO-MERCEARIA', 'MFR-DEMO-LIMPEZA'
          )
    ) THEN
        RAISE EXCEPTION 'Refusing to use category codes outside the reserved demo namespace';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM product product
        JOIN (VALUES
            ('MFR-DEMO-BANANA', md5('marketfastroute.demo.product.MFR-DEMO-BANANA')::uuid),
            ('MFR-DEMO-MACA', md5('marketfastroute.demo.product.MFR-DEMO-MACA')::uuid),
            ('MFR-DEMO-PAO', md5('marketfastroute.demo.product.MFR-DEMO-PAO')::uuid),
            ('MFR-DEMO-BOLO', md5('marketfastroute.demo.product.MFR-DEMO-BOLO')::uuid),
            ('MFR-DEMO-AGUA', md5('marketfastroute.demo.product.MFR-DEMO-AGUA')::uuid),
            ('MFR-DEMO-SUCO', md5('marketfastroute.demo.product.MFR-DEMO-SUCO')::uuid),
            ('MFR-DEMO-LEITE', md5('marketfastroute.demo.product.MFR-DEMO-LEITE')::uuid),
            ('MFR-DEMO-QUEIJO', md5('marketfastroute.demo.product.MFR-DEMO-QUEIJO')::uuid),
            ('MFR-DEMO-ARROZ', md5('marketfastroute.demo.product.MFR-DEMO-ARROZ')::uuid),
            ('MFR-DEMO-FEIJAO', md5('marketfastroute.demo.product.MFR-DEMO-FEIJAO')::uuid),
            ('MFR-DEMO-DETERGENTE', md5('marketfastroute.demo.product.MFR-DEMO-DETERGENTE')::uuid),
            ('MFR-DEMO-SABAO', md5('marketfastroute.demo.product.MFR-DEMO-SABAO')::uuid)
        ) AS expected(sku, id) ON product.sku = expected.sku
        WHERE product.id <> expected.id
    ) OR EXISTS (
        SELECT 1 FROM product WHERE sku LIKE 'MFR-DEMO-%'
          AND sku NOT IN (
            'MFR-DEMO-BANANA', 'MFR-DEMO-MACA', 'MFR-DEMO-PAO', 'MFR-DEMO-BOLO',
            'MFR-DEMO-AGUA', 'MFR-DEMO-SUCO', 'MFR-DEMO-LEITE', 'MFR-DEMO-QUEIJO',
            'MFR-DEMO-ARROZ', 'MFR-DEMO-FEIJAO', 'MFR-DEMO-DETERGENTE', 'MFR-DEMO-SABAO'
          )
    ) THEN
        RAISE EXCEPTION 'Refusing to use product SKUs outside the reserved demo namespace';
    END IF;
END;
$$;

INSERT INTO store (id, name, code, address, city, state, active)
VALUES (
    md5('marketfastroute.demo.store')::uuid,
    'Mercado Aurora — demonstração fictícia',
    'MFR-DEMO-AURORA',
    'Endereço demonstrativo',
    'Cidade Fictícia',
    'SP',
    TRUE
)
ON CONFLICT (id) DO UPDATE
SET name = EXCLUDED.name,
    code = EXCLUDED.code,
    address = EXCLUDED.address,
    city = EXCLUDED.city,
    state = EXCLUDED.state,
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO category (id, name, code, active)
SELECT md5('marketfastroute.demo.category.' || demo.code)::uuid,
       demo.name,
       demo.code,
       TRUE
FROM (VALUES
    ('MFR-DEMO-HORTIFRUTI', 'Hortifruti'),
    ('MFR-DEMO-PADARIA', 'Padaria'),
    ('MFR-DEMO-BEBIDAS', 'Bebidas'),
    ('MFR-DEMO-LATICINIOS', 'Laticínios'),
    ('MFR-DEMO-MERCEARIA', 'Mercearia'),
    ('MFR-DEMO-LIMPEZA', 'Limpeza')
) AS demo(code, name)
ON CONFLICT (code) DO UPDATE
SET name = EXCLUDED.name,
    active = TRUE;

WITH demo_products(sku, name, category_code) AS (
    VALUES
        ('MFR-DEMO-BANANA', 'Banana prata', 'MFR-DEMO-HORTIFRUTI'),
        ('MFR-DEMO-MACA', 'Maçã gala', 'MFR-DEMO-HORTIFRUTI'),
        ('MFR-DEMO-PAO', 'Pão integral', 'MFR-DEMO-PADARIA'),
        ('MFR-DEMO-BOLO', 'Bolo de milho', 'MFR-DEMO-PADARIA'),
        ('MFR-DEMO-AGUA', 'Água mineral', 'MFR-DEMO-BEBIDAS'),
        ('MFR-DEMO-SUCO', 'Suco de laranja', 'MFR-DEMO-BEBIDAS'),
        ('MFR-DEMO-LEITE', 'Leite integral', 'MFR-DEMO-LATICINIOS'),
        ('MFR-DEMO-QUEIJO', 'Queijo prato', 'MFR-DEMO-LATICINIOS'),
        ('MFR-DEMO-ARROZ', 'Arroz', 'MFR-DEMO-MERCEARIA'),
        ('MFR-DEMO-FEIJAO', 'Feijão carioca', 'MFR-DEMO-MERCEARIA'),
        ('MFR-DEMO-DETERGENTE', 'Detergente neutro', 'MFR-DEMO-LIMPEZA'),
        ('MFR-DEMO-SABAO', 'Sabão em pó', 'MFR-DEMO-LIMPEZA')
)
INSERT INTO product (id, category_id, sku, name, description, active)
SELECT md5('marketfastroute.demo.product.' || demo.sku)::uuid,
       category.id,
       demo.sku,
       demo.name,
       'Item sintético da base de demonstração.',
       TRUE
FROM demo_products demo
JOIN category ON category.code = demo.category_code
ON CONFLICT (sku) DO UPDATE
SET category_id = EXCLUDED.category_id,
    name = EXCLUDED.name,
    description = EXCLUDED.description,
    active = TRUE,
    updated_at = CURRENT_TIMESTAMP;

INSERT INTO store_product (id, store_id, product_id, active)
SELECT md5('marketfastroute.demo.store-product.' || product.sku)::uuid,
       store.id,
       product.id,
       TRUE
FROM store
JOIN product ON product.sku IN (
    'MFR-DEMO-BANANA', 'MFR-DEMO-MACA', 'MFR-DEMO-PAO', 'MFR-DEMO-BOLO',
    'MFR-DEMO-AGUA', 'MFR-DEMO-SUCO', 'MFR-DEMO-LEITE', 'MFR-DEMO-QUEIJO',
    'MFR-DEMO-ARROZ', 'MFR-DEMO-FEIJAO', 'MFR-DEMO-DETERGENTE', 'MFR-DEMO-SABAO'
)
WHERE store.code = 'MFR-DEMO-AURORA'
ON CONFLICT (store_id, product_id) DO UPDATE
SET active = TRUE;

INSERT INTO store_map (id, store_id, version, name, width, height, scale_meters_per_unit, status)
SELECT md5('marketfastroute.demo.map')::uuid,
       store.id,
       1,
       'Planta demonstrativa',
       240,
       160,
       0.5,
       'DRAFT'
FROM store
WHERE store.code = 'MFR-DEMO-AURORA'
ON CONFLICT (store_id, version) DO UPDATE
SET name = EXCLUDED.name,
    width = EXCLUDED.width,
    height = EXCLUDED.height,
    scale_meters_per_unit = EXCLUDED.scale_meters_per_unit,
    updated_at = CURRENT_TIMESTAMP;

WITH map_ref AS (
    SELECT store_map.id AS map_id
    FROM store_map
    JOIN store ON store.id = store_map.store_id
    WHERE store.code = 'MFR-DEMO-AURORA'
      AND store_map.version = 1
), demo_sectors(code, name, x, y, width, height) AS (
    VALUES
        ('HORTIFRUTI', 'Hortifruti', 10, 10, 60, 34),
        ('PADARIA', 'Padaria', 90, 10, 60, 34),
        ('BEBIDAS', 'Bebidas', 155, 10, 75, 34),
        ('LATICINIOS', 'Laticínios', 10, 116, 60, 34),
        ('MERCEARIA', 'Mercearia', 90, 116, 60, 34),
        ('LIMPEZA', 'Limpeza', 155, 116, 75, 34)
)
INSERT INTO sector (id, map_id, name, code, x, y, width, height, rotation, active)
SELECT md5('marketfastroute.demo.sector.' || demo.code)::uuid,
       map_ref.map_id,
       demo.name,
       demo.code,
       demo.x,
       demo.y,
       demo.width,
       demo.height,
       0,
       TRUE
FROM map_ref
CROSS JOIN demo_sectors demo
ON CONFLICT (map_id, code) DO UPDATE
SET name = EXCLUDED.name,
    x = EXCLUDED.x,
    y = EXCLUDED.y,
    width = EXCLUDED.width,
    height = EXCLUDED.height,
    rotation = EXCLUDED.rotation,
    active = TRUE;

WITH map_ref AS (
    SELECT store_map.id AS map_id
    FROM store_map
    JOIN store ON store.id = store_map.store_id
    WHERE store.code = 'MFR-DEMO-AURORA'
      AND store_map.version = 1
), demo_aisles(code, name, sector_code, x, y) AS (
    VALUES
        ('A01', 'Corredor 01', 'HORTIFRUTI', 18, 23),
        ('A02', 'Corredor 02', 'PADARIA', 98, 23),
        ('A03', 'Corredor 03', 'BEBIDAS', 178, 23),
        ('A04', 'Corredor 04', 'LATICINIOS', 18, 117),
        ('A05', 'Corredor 05', 'MERCEARIA', 98, 117),
        ('A06', 'Corredor 06', 'LIMPEZA', 178, 117)
)
INSERT INTO aisle (id, map_id, sector_id, code, name, x, y, width, height, rotation, active)
SELECT md5('marketfastroute.demo.aisle.' || demo.code)::uuid,
       map_ref.map_id,
       sector.id,
       demo.code,
       demo.name,
       demo.x,
       demo.y,
       44,
       5,
       0,
       TRUE
FROM map_ref
CROSS JOIN demo_aisles demo
JOIN sector ON sector.map_id = map_ref.map_id
           AND sector.code = demo.sector_code
ON CONFLICT (map_id, code) DO UPDATE
SET sector_id = EXCLUDED.sector_id,
    name = EXCLUDED.name,
    x = EXCLUDED.x,
    y = EXCLUDED.y,
    width = EXCLUDED.width,
    height = EXCLUDED.height,
    rotation = EXCLUDED.rotation,
    active = TRUE;

WITH map_ref AS (
    SELECT store_map.id AS map_id
    FROM store_map
    JOIN store ON store.id = store_map.store_id
    WHERE store.code = 'MFR-DEMO-AURORA'
      AND store_map.version = 1
), demo_blocks(code, name, aisle_code, sector_code, x, y) AS (
    VALUES
        ('H01-B01', 'Hortifruti 1', 'A01', 'HORTIFRUTI', 18, 30),
        ('H01-B02', 'Hortifruti 2', 'A01', 'HORTIFRUTI', 42, 30),
        ('H02-B01', 'Padaria 1', 'A02', 'PADARIA', 98, 30),
        ('H02-B02', 'Padaria 2', 'A02', 'PADARIA', 122, 30),
        ('H03-B01', 'Bebidas 1', 'A03', 'BEBIDAS', 178, 30),
        ('H03-B02', 'Bebidas 2', 'A03', 'BEBIDAS', 202, 30),
        ('H04-B01', 'Laticínios 1', 'A04', 'LATICINIOS', 18, 126),
        ('H04-B02', 'Laticínios 2', 'A04', 'LATICINIOS', 42, 126),
        ('H05-B01', 'Mercearia 1', 'A05', 'MERCEARIA', 98, 126),
        ('H05-B02', 'Mercearia 2', 'A05', 'MERCEARIA', 122, 126),
        ('H06-B01', 'Limpeza 1', 'A06', 'LIMPEZA', 178, 126),
        ('H06-B02', 'Limpeza 2', 'A06', 'LIMPEZA', 202, 126)
)
INSERT INTO shelf_block (id, map_id, sector_id, aisle_id, code, name, x, y, width, height, rotation, active)
SELECT md5('marketfastroute.demo.shelf.' || demo.code)::uuid,
       map_ref.map_id,
       sector.id,
       aisle.id,
       demo.code,
       demo.name,
       demo.x,
       demo.y,
       18,
       5,
       0,
       TRUE
FROM map_ref
CROSS JOIN demo_blocks demo
JOIN sector ON sector.map_id = map_ref.map_id
           AND sector.code = demo.sector_code
JOIN aisle ON aisle.map_id = map_ref.map_id
          AND aisle.code = demo.aisle_code
ON CONFLICT (map_id, code) DO UPDATE
SET sector_id = EXCLUDED.sector_id,
    aisle_id = EXCLUDED.aisle_id,
    name = EXCLUDED.name,
    x = EXCLUDED.x,
    y = EXCLUDED.y,
    width = EXCLUDED.width,
    height = EXCLUDED.height,
    rotation = EXCLUDED.rotation,
    active = TRUE;

WITH map_ref AS (
    SELECT store_map.id AS map_id
    FROM store_map
    JOIN store ON store.id = store_map.store_id
    WHERE store.code = 'MFR-DEMO-AURORA'
      AND store_map.version = 1
)
INSERT INTO map_node (id, map_id, type, x, y, label, active)
SELECT md5('marketfastroute.demo.node.' || row_index || '.' || col_index)::uuid,
       map_ref.map_id,
       CASE
           WHEN row_index = 1 AND col_index = 0 THEN 'ENTRANCE'
           WHEN row_index = 1 AND col_index = 7 THEN 'CHECKOUT'
           WHEN (row_index IN (0, 3) AND col_index IN (1, 3, 5)) THEN 'PRODUCT_ACCESS'
           WHEN row_index IN (1, 2) OR col_index IN (0, 7) THEN 'INTERSECTION'
           ELSE 'PATH'
       END,
       12 + col_index * 30,
       14 + row_index * 42,
       CASE
           WHEN row_index = 1 AND col_index = 0 THEN 'Entrada'
           WHEN row_index = 1 AND col_index = 7 THEN 'Caixas'
           WHEN (row_index IN (0, 3) AND col_index IN (1, 3, 5)) THEN 'Acesso a produtos'
           ELSE NULL
       END,
       TRUE
FROM map_ref
    CROSS JOIN generate_series(0, 3) AS row_axis(row_index)
    CROSS JOIN generate_series(0, 7) AS column_axis(col_index)
ON CONFLICT (id) DO UPDATE
SET map_id = EXCLUDED.map_id,
    type = EXCLUDED.type,
    x = EXCLUDED.x,
    y = EXCLUDED.y,
    label = EXCLUDED.label,
    active = TRUE;

WITH map_ref AS (
    SELECT store_map.id AS map_id
    FROM store_map
    JOIN store ON store.id = store_map.store_id
    WHERE store.code = 'MFR-DEMO-AURORA'
      AND store_map.version = 1
)
INSERT INTO point_of_interest (id, map_id, navigation_node_id, type, name, x, y, active)
SELECT md5('marketfastroute.demo.poi.' || demo.code)::uuid,
       map_ref.map_id,
       md5('marketfastroute.demo.node.' || demo.row_index || '.' || demo.col_index)::uuid,
       demo.type,
       demo.name,
       12 + demo.col_index * 30,
       14 + demo.row_index * 42,
       TRUE
FROM map_ref
CROSS JOIN (VALUES
    ('ENTRANCE', 'ENTRANCE', 'Entrada principal', 1, 0),
    ('CHECKOUT', 'CHECKOUT', 'Caixas', 1, 7),
    ('CARTS', 'CART', 'Carrinhos', 1, 1)
) AS demo(code, type, name, row_index, col_index)
ON CONFLICT (id) DO UPDATE
SET map_id = EXCLUDED.map_id,
    navigation_node_id = EXCLUDED.navigation_node_id,
    type = EXCLUDED.type,
    name = EXCLUDED.name,
    x = EXCLUDED.x,
    y = EXCLUDED.y,
    active = TRUE;

WITH map_ref AS (
    SELECT store_map.id AS map_id
    FROM store_map
    JOIN store ON store.id = store_map.store_id
    WHERE store.code = 'MFR-DEMO-AURORA'
      AND store_map.version = 1
), links(from_row, from_col, to_row, to_col) AS (
    SELECT row_index, col_index, row_index, col_index + 1
    FROM generate_series(0, 3) AS row_axis(row_index)
    CROSS JOIN generate_series(0, 6) AS column_axis(col_index)
    UNION ALL
    SELECT row_index, col_index, row_index + 1, col_index
    FROM generate_series(0, 2) AS row_axis(row_index)
    CROSS JOIN (VALUES (0), (2), (4), (6), (7)) AS column_axis(col_index)
)
INSERT INTO map_edge (id, map_id, from_node_id, to_node_id, distance_meters, bidirectional, active)
SELECT md5('marketfastroute.demo.edge.' || links.from_row || '.' || links.from_col || '.'
       || links.to_row || '.' || links.to_col)::uuid,
       map_ref.map_id,
       md5('marketfastroute.demo.node.' || links.from_row || '.' || links.from_col)::uuid,
       md5('marketfastroute.demo.node.' || links.to_row || '.' || links.to_col)::uuid,
       CASE WHEN links.from_row = links.to_row THEN 15 ELSE 21 END,
       TRUE,
       TRUE
FROM map_ref
CROSS JOIN links
ON CONFLICT (map_id, from_node_id, to_node_id) DO UPDATE
SET distance_meters = EXCLUDED.distance_meters,
    bidirectional = TRUE,
    active = TRUE;

WITH map_ref AS (
    SELECT store_map.id AS map_id, store_map.store_id
    FROM store_map
    JOIN store ON store.id = store_map.store_id
    WHERE store.code = 'MFR-DEMO-AURORA'
      AND store_map.version = 1
), demo_locations(sku, sector_code, aisle_code, shelf_code, row_index, col_index) AS (
    VALUES
        ('MFR-DEMO-BANANA', 'HORTIFRUTI', 'A01', 'H01-B01', 0, 1),
        ('MFR-DEMO-MACA', 'HORTIFRUTI', 'A01', 'H01-B02', 0, 1),
        ('MFR-DEMO-PAO', 'PADARIA', 'A02', 'H02-B01', 0, 3),
        ('MFR-DEMO-BOLO', 'PADARIA', 'A02', 'H02-B02', 0, 3),
        ('MFR-DEMO-AGUA', 'BEBIDAS', 'A03', 'H03-B01', 0, 5),
        ('MFR-DEMO-SUCO', 'BEBIDAS', 'A03', 'H03-B02', 0, 5),
        ('MFR-DEMO-LEITE', 'LATICINIOS', 'A04', 'H04-B01', 3, 1),
        ('MFR-DEMO-QUEIJO', 'LATICINIOS', 'A04', 'H04-B02', 3, 1),
        ('MFR-DEMO-ARROZ', 'MERCEARIA', 'A05', 'H05-B01', 3, 3),
        ('MFR-DEMO-FEIJAO', 'MERCEARIA', 'A05', 'H05-B02', 3, 3),
        ('MFR-DEMO-DETERGENTE', 'LIMPEZA', 'A06', 'H06-B01', 3, 5),
        ('MFR-DEMO-SABAO', 'LIMPEZA', 'A06', 'H06-B02', 3, 5)
)
INSERT INTO product_location (
    id, store_id, store_product_id, map_id, sector_id, aisle_id, shelf_block_id,
    module, shelf_level, x, y, navigation_node_id, primary_location, active
)
SELECT md5('marketfastroute.demo.location.' || demo.sku)::uuid,
       map_ref.store_id,
       store_product.id,
       map_ref.map_id,
       sector.id,
       aisle.id,
       shelf_block.id,
       'Módulo 1',
       1,
       12 + demo.col_index * 30,
       14 + demo.row_index * 42,
       md5('marketfastroute.demo.node.' || demo.row_index || '.' || demo.col_index)::uuid,
       TRUE,
       TRUE
FROM map_ref
CROSS JOIN demo_locations demo
JOIN store_product ON store_product.store_id = map_ref.store_id
JOIN product ON product.id = store_product.product_id
             AND product.sku = demo.sku
JOIN sector ON sector.map_id = map_ref.map_id
            AND sector.code = demo.sector_code
JOIN aisle ON aisle.map_id = map_ref.map_id
           AND aisle.code = demo.aisle_code
JOIN shelf_block ON shelf_block.map_id = map_ref.map_id
                 AND shelf_block.code = demo.shelf_code
ON CONFLICT (id) DO UPDATE
SET store_id = EXCLUDED.store_id,
    store_product_id = EXCLUDED.store_product_id,
    map_id = EXCLUDED.map_id,
    sector_id = EXCLUDED.sector_id,
    aisle_id = EXCLUDED.aisle_id,
    shelf_block_id = EXCLUDED.shelf_block_id,
    module = EXCLUDED.module,
    shelf_level = EXCLUDED.shelf_level,
    x = EXCLUDED.x,
    y = EXCLUDED.y,
    navigation_node_id = EXCLUDED.navigation_node_id,
    primary_location = TRUE,
    active = TRUE;

COMMIT;
