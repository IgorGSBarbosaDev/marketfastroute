BEGIN;

DO $$
DECLARE
    demo_store_id UUID := md5('marketfastroute.demo.store')::uuid;
    demo_map_id UUID := md5('marketfastroute.demo.map.v3')::uuid;
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
        WHERE (store_id = demo_store_id AND version = 3
               AND (id <> demo_map_id OR status <> 'DRAFT'))
           OR (id = demo_map_id AND (store_id <> demo_store_id OR version <> 3))
    ) THEN
        RAISE EXCEPTION 'Refusing to refresh Mercado Aurora: version 3 must belong to the demo store and remain a draft';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM map_node node
        JOIN (
            SELECT 'ENTRANCE' AS node_code
            UNION ALL SELECT 'CARTS'
            UNION ALL SELECT 'EXIT'
            UNION ALL SELECT 'CHECKOUT'
            UNION ALL SELECT 'CROSS-A' || lpad(sequence::text, 2, '0')
            FROM generate_series(1, 14) AS aisle(sequence)
            UNION ALL SELECT 'AISLE-A' || lpad(sequence::text, 2, '0') || '-ENTRY'
            FROM generate_series(1, 14) AS aisle(sequence)
            UNION ALL SELECT 'AISLE-A' || lpad(sequence::text, 2, '0') || '-FAR'
            FROM generate_series(1, 14) AS aisle(sequence)
            UNION ALL SELECT 'BOTTOM-A' || lpad(sequence::text, 2, '0')
            FROM generate_series(9, 14) AS aisle(sequence)
        ) AS expected ON node.id = md5('marketfastroute.demo.v3.node.' || expected.node_code)::uuid
        WHERE node.map_id <> demo_map_id
    ) THEN
        RAISE EXCEPTION 'Refusing to overwrite a navigation node outside demo map version 3';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM point_of_interest point
        CROSS JOIN (VALUES ('ENTRANCE'), ('CHECKOUT'), ('CARTS')) AS demo(code)
        WHERE point.id = md5('marketfastroute.demo.v3.poi.' || demo.code)::uuid
          AND point.map_id <> demo_map_id
    ) THEN
        RAISE EXCEPTION 'Refusing to overwrite a point of interest outside demo map version 3';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM product_location location
        JOIN (VALUES
            ('MFR-DEMO-BANANA'), ('MFR-DEMO-MACA'), ('MFR-DEMO-PAO'), ('MFR-DEMO-BOLO'),
            ('MFR-DEMO-AGUA'), ('MFR-DEMO-SUCO'), ('MFR-DEMO-LEITE'), ('MFR-DEMO-QUEIJO'),
            ('MFR-DEMO-ERVILHA'), ('MFR-DEMO-SORVETE'),
            ('MFR-DEMO-ARROZ'), ('MFR-DEMO-FEIJAO'), ('MFR-DEMO-DETERGENTE'), ('MFR-DEMO-SABAO')
        ) AS demo(sku) ON location.id = md5('marketfastroute.demo.v3.location.' || demo.sku)::uuid
        WHERE location.store_id <> demo_store_id OR location.map_id <> demo_map_id
    ) THEN
        RAISE EXCEPTION 'Refusing to overwrite a product location outside demo map version 3';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM sector
        JOIN (VALUES
            ('HORTIFRUTI'), ('LATICINIOS'), ('PADARIA'), ('BEBIDAS'),
            ('MERCEARIA'), ('LIMPEZA'), ('CONGELADOS')
        ) AS demo(code)
          ON sector.id = md5('marketfastroute.demo.v3.sector.' || demo.code)::uuid
        WHERE sector.map_id <> demo_map_id
    ) OR EXISTS (
        SELECT 1
        FROM aisle
        CROSS JOIN generate_series(1, 17) AS demo(sequence)
        WHERE aisle.id = md5('marketfastroute.demo.v3.aisle.A' || lpad(demo.sequence::text, 2, '0'))::uuid
          AND aisle.map_id <> demo_map_id
    ) OR EXISTS (
        SELECT 1
        FROM shelf_block
        JOIN (VALUES
            ('H01'), ('H02'), ('H03'), ('P01'), ('P02'), ('P03'),
            ('BE01'), ('BE02'), ('BE03'), ('HZ01'), ('HZ02'), ('HZ03'),
            ('M01'), ('M02'), ('M03'), ('LT01'), ('LT02'), ('LT03'),
            ('C01'), ('C02'), ('C03')
        ) AS demo(code)
          ON shelf_block.id = md5('marketfastroute.demo.v3.shelf.' || demo.code)::uuid
        WHERE shelf_block.map_id <> demo_map_id
    ) THEN
        RAISE EXCEPTION 'Refusing to overwrite map structure outside demo map version 3';
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
            ('MFR-DEMO-LIMPEZA', md5('marketfastroute.demo.category.MFR-DEMO-LIMPEZA')::uuid),
            ('MFR-DEMO-CONGELADOS', md5('marketfastroute.demo.category.MFR-DEMO-CONGELADOS')::uuid)
        ) AS expected(code, id) ON category.code = expected.code
        WHERE category.id <> expected.id
    ) OR EXISTS (
        SELECT 1 FROM category WHERE code LIKE 'MFR-DEMO-%'
          AND code NOT IN (
            'MFR-DEMO-HORTIFRUTI', 'MFR-DEMO-PADARIA', 'MFR-DEMO-BEBIDAS',
            'MFR-DEMO-LATICINIOS', 'MFR-DEMO-MERCEARIA', 'MFR-DEMO-LIMPEZA',
            'MFR-DEMO-CONGELADOS'
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
            ('MFR-DEMO-ERVILHA', md5('marketfastroute.demo.product.MFR-DEMO-ERVILHA')::uuid),
            ('MFR-DEMO-SORVETE', md5('marketfastroute.demo.product.MFR-DEMO-SORVETE')::uuid),
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
            'MFR-DEMO-ERVILHA', 'MFR-DEMO-SORVETE',
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
    ('MFR-DEMO-LIMPEZA', 'Limpeza'),
    ('MFR-DEMO-CONGELADOS', 'Congelados')
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
        ('MFR-DEMO-ERVILHA', 'Ervilha congelada', 'MFR-DEMO-CONGELADOS'),
        ('MFR-DEMO-SORVETE', 'Sorvete de creme', 'MFR-DEMO-CONGELADOS'),
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
    'MFR-DEMO-ERVILHA', 'MFR-DEMO-SORVETE',
    'MFR-DEMO-ARROZ', 'MFR-DEMO-FEIJAO', 'MFR-DEMO-DETERGENTE', 'MFR-DEMO-SABAO'
)
WHERE store.code = 'MFR-DEMO-AURORA'
ON CONFLICT (store_id, product_id) DO UPDATE
SET active = TRUE;

CREATE TEMP TABLE demo_target_map (store_id UUID NOT NULL, map_id UUID NOT NULL) ON COMMIT DROP;

WITH demo_store AS (
    SELECT id AS store_id
    FROM store
    WHERE code = 'MFR-DEMO-AURORA'
), map_upsert AS (
    INSERT INTO store_map (
        id, store_id, version, name, width, height, scale_meters_per_unit, status
    )
    SELECT md5('marketfastroute.demo.map.v3')::uuid,
           demo_store.store_id,
           3,
           'Planta demonstrativa — entrada, perecíveis e corredores',
           240,
           160,
           0.25,
           'DRAFT'
    FROM demo_store
    ON CONFLICT (store_id, version) DO UPDATE
    SET name = EXCLUDED.name,
        width = EXCLUDED.width,
        height = EXCLUDED.height,
        scale_meters_per_unit = EXCLUDED.scale_meters_per_unit,
        updated_at = CURRENT_TIMESTAMP
    WHERE store_map.status = 'DRAFT'
    RETURNING id, store_id
)
INSERT INTO demo_target_map (map_id, store_id)
SELECT id, store_id
FROM map_upsert;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM demo_target_map) THEN
        RAISE EXCEPTION 'Demo map version 3 is not an editable draft';
    END IF;
END;
$$;

CREATE TEMP TABLE demo_sectors (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    x NUMERIC NOT NULL,
    y NUMERIC NOT NULL,
    width NUMERIC NOT NULL,
    height NUMERIC NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_sectors (code, name, x, y, width, height) VALUES
    ('HORTIFRUTI', 'Hortifruti', 8, 8, 54, 50),
    ('PADARIA', 'Padaria', 66, 8, 54, 50),
    ('BEBIDAS', 'Bebidas', 124, 8, 54, 50),
    ('LIMPEZA', 'Higiene e Limpeza', 182, 8, 54, 50),
    ('MERCEARIA', 'Mercearia', 8, 80, 72, 60),
    ('LATICINIOS', 'Laticínios', 84, 80, 72, 60),
    ('CONGELADOS', 'Congelados', 160, 80, 72, 60);

INSERT INTO sector (id, map_id, name, code, x, y, width, height, rotation, active)
SELECT md5('marketfastroute.demo.v3.sector.' || demo.code)::uuid,
       map_ref.map_id, demo.name, demo.code, demo.x, demo.y, demo.width, demo.height, 0, TRUE
FROM demo_target_map map_ref
CROSS JOIN demo_sectors demo
ON CONFLICT (map_id, code) DO UPDATE
SET name = EXCLUDED.name,
    x = EXCLUDED.x,
    y = EXCLUDED.y,
    width = EXCLUDED.width,
    height = EXCLUDED.height,
    rotation = EXCLUDED.rotation,
    active = TRUE;

CREATE TEMP TABLE demo_aisles (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    sector_code TEXT,
    x NUMERIC NOT NULL,
    y NUMERIC NOT NULL,
    width NUMERIC NOT NULL,
    height NUMERIC NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_aisles (code, name, sector_code, x, y, width, height) VALUES
    ('A01', 'Corredor 1 de Hortifruti', 'HORTIFRUTI', 20, 20, 10, 42),
    ('A02', 'Corredor 2 de Hortifruti', 'HORTIFRUTI', 40, 20, 10, 42),
    ('A03', 'Corredor 1 de Padaria', 'PADARIA', 78, 20, 10, 42),
    ('A04', 'Corredor 2 de Padaria', 'PADARIA', 98, 20, 10, 42),
    ('A05', 'Corredor 1 de Bebidas', 'BEBIDAS', 136, 20, 10, 42),
    ('A06', 'Corredor 2 de Bebidas', 'BEBIDAS', 156, 20, 10, 42),
    ('A07', 'Corredor 1 de Higiene e Limpeza', 'LIMPEZA', 194, 20, 10, 42),
    ('A08', 'Corredor 2 de Higiene e Limpeza', 'LIMPEZA', 214, 20, 10, 42),
    ('A09', 'Corredor 1 de Mercearia', 'MERCEARIA', 25, 69, 10, 77),
    ('A10', 'Corredor 2 de Mercearia', 'MERCEARIA', 48, 69, 10, 77),
    ('A11', 'Corredor 1 de Laticínios', 'LATICINIOS', 101, 69, 10, 77),
    ('A12', 'Corredor 2 de Laticínios', 'LATICINIOS', 124, 69, 10, 77),
    ('A13', 'Corredor 1 de Congelados', 'CONGELADOS', 177, 69, 10, 77),
    ('A14', 'Corredor 2 de Congelados', 'CONGELADOS', 200, 69, 10, 77),
    ('A15', 'Travessa principal', NULL, 8, 58, 230, 22),
    ('A16', 'Ligação lateral às caixas', NULL, 232, 69, 6, 82),
    ('A17', 'Travessa dos caixas', NULL, 8, 146, 230, 10);

UPDATE product_location location
SET sector_id = NULL,
    aisle_id = NULL,
    shelf_block_id = NULL
FROM demo_target_map map_ref
WHERE location.map_id = map_ref.map_id
  AND location.id IN (
      SELECT md5('marketfastroute.demo.v3.location.' || demo.sku)::uuid
      FROM (VALUES
          ('MFR-DEMO-BANANA'), ('MFR-DEMO-MACA'), ('MFR-DEMO-PAO'), ('MFR-DEMO-BOLO'),
          ('MFR-DEMO-AGUA'), ('MFR-DEMO-SUCO'), ('MFR-DEMO-LEITE'), ('MFR-DEMO-QUEIJO'),
          ('MFR-DEMO-ERVILHA'), ('MFR-DEMO-SORVETE'), ('MFR-DEMO-ARROZ'), ('MFR-DEMO-FEIJAO'),
          ('MFR-DEMO-DETERGENTE'), ('MFR-DEMO-SABAO')
      ) AS demo(sku)
  );

UPDATE shelf_block block
SET sector_id = NULL,
    aisle_id = NULL
FROM demo_target_map map_ref
WHERE block.map_id = map_ref.map_id
  AND block.code IN (
      'T-H01', 'T-H02', 'T-H03', 'T-H04',
      'T-L01', 'T-L02', 'T-L03', 'T-L04',
      'T-P01', 'T-P02', 'T-P03', 'T-P04',
      'T-B01', 'T-B02', 'T-B03', 'T-B04',
      'B-M01', 'B-M02', 'B-M03', 'B-M04',
      'B-L01', 'B-L02', 'B-L03', 'B-L04',
      'B-C01', 'B-C02', 'B-C03', 'B-C04'
  );

INSERT INTO aisle (id, map_id, sector_id, code, name, x, y, width, height, rotation, active)
SELECT md5('marketfastroute.demo.v3.aisle.' || demo.code)::uuid,
       map_ref.map_id, sector.id, demo.code, demo.name,
       demo.x, demo.y, demo.width, demo.height, 0, TRUE
FROM demo_target_map map_ref
CROSS JOIN demo_aisles demo
LEFT JOIN sector ON sector.map_id = map_ref.map_id
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

CREATE TEMP TABLE demo_shelves (
    code TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    aisle_code TEXT NOT NULL,
    sector_code TEXT NOT NULL,
    x NUMERIC NOT NULL,
    y NUMERIC NOT NULL,
    width NUMERIC NOT NULL,
    height NUMERIC NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_shelves (code, name, aisle_code, sector_code, x, y, width, height) VALUES
    ('H01', 'Gôndola H01 · Hortifruti', 'A01', 'HORTIFRUTI', 11, 22, 8, 32),
    ('H02', 'Gôndola H02 · Hortifruti', 'A01', 'HORTIFRUTI', 31, 22, 8, 32),
    ('H03', 'Gôndola H03 · Hortifruti', 'A02', 'HORTIFRUTI', 51, 22, 8, 32),
    ('P01', 'Gôndola P01 · Padaria', 'A03', 'PADARIA', 69, 22, 8, 32),
    ('P02', 'Gôndola P02 · Padaria', 'A03', 'PADARIA', 89, 22, 8, 32),
    ('P03', 'Gôndola P03 · Padaria', 'A04', 'PADARIA', 109, 22, 8, 32),
    ('BE01', 'Gôndola BE01 · Bebidas', 'A05', 'BEBIDAS', 127, 22, 8, 32),
    ('BE02', 'Gôndola BE02 · Bebidas', 'A05', 'BEBIDAS', 147, 22, 8, 32),
    ('BE03', 'Gôndola BE03 · Bebidas', 'A06', 'BEBIDAS', 167, 22, 8, 32),
    ('HZ01', 'Gôndola HZ01 · Higiene e Limpeza', 'A07', 'LIMPEZA', 185, 22, 8, 32),
    ('HZ02', 'Gôndola HZ02 · Higiene e Limpeza', 'A07', 'LIMPEZA', 205, 22, 8, 32),
    ('HZ03', 'Gôndola HZ03 · Higiene e Limpeza', 'A08', 'LIMPEZA', 225, 22, 8, 32),
    ('M01', 'Gôndola M01 · Mercearia', 'A09', 'MERCEARIA', 14, 96, 9, 34),
    ('M02', 'Gôndola M02 · Mercearia', 'A09', 'MERCEARIA', 37, 96, 9, 34),
    ('M03', 'Gôndola M03 · Mercearia', 'A10', 'MERCEARIA', 60, 96, 9, 34),
    ('LT01', 'Gôndola LT01 · Laticínios', 'A11', 'LATICINIOS', 90, 96, 9, 34),
    ('LT02', 'Gôndola LT02 · Laticínios', 'A11', 'LATICINIOS', 113, 96, 9, 34),
    ('LT03', 'Gôndola LT03 · Laticínios', 'A12', 'LATICINIOS', 136, 96, 9, 34),
    ('C01', 'Gôndola C01 · Congelados', 'A13', 'CONGELADOS', 166, 96, 9, 34),
    ('C02', 'Gôndola C02 · Congelados', 'A13', 'CONGELADOS', 189, 96, 9, 34),
    ('C03', 'Gôndola C03 · Congelados', 'A14', 'CONGELADOS', 212, 96, 9, 34);

INSERT INTO shelf_block (id, map_id, sector_id, aisle_id, code, name, x, y, width, height, rotation, active)
SELECT md5('marketfastroute.demo.v3.shelf.' || demo.code)::uuid,
       map_ref.map_id, sector.id, aisle.id, demo.code, demo.name,
       demo.x, demo.y, demo.width, demo.height, 0, TRUE
FROM demo_target_map map_ref
CROSS JOIN demo_shelves demo
JOIN sector ON sector.map_id = map_ref.map_id AND sector.code = demo.sector_code
JOIN aisle ON aisle.map_id = map_ref.map_id AND aisle.code = demo.aisle_code
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

CREATE TEMP TABLE demo_aisle_paths (
    aisle_code TEXT PRIMARY KEY,
    center_x NUMERIC NOT NULL,
    entry_y NUMERIC NOT NULL,
    far_y NUMERIC NOT NULL,
    lower_row BOOLEAN NOT NULL
) ON COMMIT DROP;

INSERT INTO demo_aisle_paths (aisle_code, center_x, entry_y, far_y, lower_row)
SELECT aisle.code,
       aisle.x + aisle.width / 2,
       CASE WHEN aisle.code <= 'A08' THEN 56 ELSE 91 END,
       CASE WHEN aisle.code <= 'A08' THEN 26 ELSE 132 END,
       aisle.code > 'A08'
FROM demo_aisles aisle
WHERE aisle.code BETWEEN 'A01' AND 'A14';

CREATE TEMP TABLE demo_nodes (
    node_code TEXT PRIMARY KEY,
    type TEXT NOT NULL,
    x NUMERIC NOT NULL,
    y NUMERIC NOT NULL,
    label TEXT
) ON COMMIT DROP;

INSERT INTO demo_nodes (node_code, type, x, y, label) VALUES
    ('ENTRANCE', 'ENTRANCE', 8, 69, 'Entrada principal'),
    ('CARTS', 'PATH', 14, 69, 'Carrinhos'),
    ('EXIT', 'INTERSECTION', 235, 69, 'Acesso lateral às caixas'),
    ('CHECKOUT', 'CHECKOUT', 235, 151, 'Caixas');

INSERT INTO demo_nodes (node_code, type, x, y, label)
SELECT 'CROSS-' || path.aisle_code, 'INTERSECTION', path.center_x, 69, 'Travessa principal'
FROM demo_aisle_paths path
UNION ALL
SELECT 'AISLE-' || path.aisle_code || '-ENTRY', 'PRODUCT_ACCESS',
       path.center_x, path.entry_y, 'Início do ' || path.aisle_code
FROM demo_aisle_paths path
UNION ALL
SELECT 'AISLE-' || path.aisle_code || '-FAR', 'PATH',
       path.center_x, path.far_y, 'Final do ' || path.aisle_code
FROM demo_aisle_paths path
UNION ALL
SELECT 'BOTTOM-' || path.aisle_code, 'PATH', path.center_x, 151, 'Travessa dos caixas'
FROM demo_aisle_paths path
WHERE path.lower_row;

INSERT INTO map_node (id, map_id, type, x, y, label, active)
SELECT md5('marketfastroute.demo.v3.node.' || node.node_code)::uuid,
       map_ref.map_id, node.type, node.x, node.y, node.label, TRUE
FROM demo_target_map map_ref
CROSS JOIN demo_nodes node
ON CONFLICT (id) DO UPDATE
SET map_id = EXCLUDED.map_id,
    type = EXCLUDED.type,
    x = EXCLUDED.x,
    y = EXCLUDED.y,
    label = EXCLUDED.label,
    active = TRUE;

INSERT INTO point_of_interest (id, map_id, navigation_node_id, type, name, x, y, active)
SELECT md5('marketfastroute.demo.v3.poi.' || demo.code)::uuid,
       map_ref.map_id,
       md5('marketfastroute.demo.v3.node.' || demo.node_code)::uuid,
       demo.type, demo.name, demo.x, demo.y, TRUE
FROM demo_target_map map_ref
CROSS JOIN (VALUES
    ('ENTRANCE', 'ENTRANCE', 'Entrada principal', 'ENTRANCE', 8, 69),
    ('CARTS', 'CART', 'Carrinhos', 'CARTS', 14, 69),
    ('CHECKOUT', 'CHECKOUT', 'Caixas', 'CHECKOUT', 235, 151)
) AS demo(code, type, name, node_code, x, y)
ON CONFLICT (id) DO UPDATE
SET map_id = EXCLUDED.map_id,
    navigation_node_id = EXCLUDED.navigation_node_id,
    type = EXCLUDED.type,
    name = EXCLUDED.name,
    x = EXCLUDED.x,
    y = EXCLUDED.y,
    active = TRUE;

CREATE TEMP TABLE demo_links (
    from_code TEXT NOT NULL,
    to_code TEXT NOT NULL,
    PRIMARY KEY (from_code, to_code)
) ON COMMIT DROP;

INSERT INTO demo_links (from_code, to_code)
SELECT 'AISLE-' || path.aisle_code || '-ENTRY',
       'AISLE-' || path.aisle_code || '-FAR'
FROM demo_aisle_paths path
UNION ALL
SELECT 'CROSS-' || path.aisle_code,
       'AISLE-' || path.aisle_code || '-ENTRY'
FROM demo_aisle_paths path
UNION ALL
SELECT 'AISLE-' || path.aisle_code || '-FAR',
       'BOTTOM-' || path.aisle_code
FROM demo_aisle_paths path
WHERE path.lower_row
UNION ALL
SELECT line.node_code, line.next_code
FROM (
    SELECT node.node_code,
           LEAD(node.node_code) OVER (ORDER BY node.x, node.node_code) AS next_code
    FROM demo_nodes node
    WHERE node.node_code IN ('ENTRANCE', 'CARTS', 'EXIT')
       OR node.node_code LIKE 'CROSS-A%'
) line
WHERE line.next_code IS NOT NULL
UNION ALL
SELECT line.node_code, line.next_code
FROM (
    SELECT node.node_code,
           LEAD(node.node_code) OVER (ORDER BY node.x, node.node_code) AS next_code
    FROM demo_nodes node
    WHERE node.node_code LIKE 'BOTTOM-A%'
       OR node.node_code = 'CHECKOUT'
) line
WHERE line.next_code IS NOT NULL
UNION ALL
SELECT 'EXIT', 'CHECKOUT';

INSERT INTO map_edge (id, map_id, from_node_id, to_node_id, distance_meters, bidirectional, active)
SELECT md5('marketfastroute.demo.v3.edge.' || link.from_code || '.' || link.to_code)::uuid,
       map_ref.map_id,
       md5('marketfastroute.demo.v3.node.' || link.from_code)::uuid,
       md5('marketfastroute.demo.v3.node.' || link.to_code)::uuid,
       GREATEST(ROUND((ABS(from_node.x - to_node.x) + ABS(from_node.y - to_node.y)) * 0.25, 2), 0.25),
       TRUE,
       TRUE
FROM demo_target_map map_ref
CROSS JOIN demo_links link
JOIN demo_nodes from_node ON from_node.node_code = link.from_code
JOIN demo_nodes to_node ON to_node.node_code = link.to_code
ON CONFLICT (map_id, from_node_id, to_node_id) DO UPDATE
SET distance_meters = EXCLUDED.distance_meters,
    bidirectional = TRUE,
    active = TRUE;

WITH legacy_nodes AS (
    SELECT md5('marketfastroute.demo.v3.node.' || groups.node_group || '.' || columns.col_index)::uuid AS id
    FROM (VALUES ('MAIN'), ('EXIT'), ('TOP'), ('LOWER')) AS groups(node_group)
    CROSS JOIN generate_series(0, 8) AS columns(col_index)
)
UPDATE map_edge edge
SET active = FALSE
FROM demo_target_map map_ref, legacy_nodes legacy
WHERE edge.map_id = map_ref.map_id
  AND (edge.from_node_id = legacy.id OR edge.to_node_id = legacy.id);

WITH legacy_nodes AS (
    SELECT md5('marketfastroute.demo.v3.node.' || groups.node_group || '.' || columns.col_index)::uuid AS id
    FROM (VALUES ('MAIN'), ('EXIT'), ('TOP'), ('LOWER')) AS groups(node_group)
    CROSS JOIN generate_series(0, 8) AS columns(col_index)
)
UPDATE map_node node
SET active = FALSE
FROM demo_target_map map_ref, legacy_nodes legacy
WHERE node.map_id = map_ref.map_id
  AND node.id = legacy.id;

UPDATE shelf_block block
SET active = FALSE
FROM demo_target_map map_ref
WHERE block.map_id = map_ref.map_id
  AND block.code IN (
      'T-H01', 'T-H02', 'T-H03', 'T-H04',
      'T-L01', 'T-L02', 'T-L03', 'T-L04',
      'T-P01', 'T-P02', 'T-P03', 'T-P04',
      'T-B01', 'T-B02', 'T-B03', 'T-B04',
      'B-M01', 'B-M02', 'B-M03', 'B-M04',
      'B-L01', 'B-L02', 'B-L03', 'B-L04',
      'B-C01', 'B-C02', 'B-C03', 'B-C04'
  );

WITH demo_locations(sku, sector_code, aisle_code, shelf_code, stop_point) AS (
    VALUES
        ('MFR-DEMO-BANANA', 'HORTIFRUTI', 'A01', 'H01', 'ENTRY'),
        ('MFR-DEMO-MACA', 'HORTIFRUTI', 'A01', 'H02', 'FAR'),
        ('MFR-DEMO-PAO', 'PADARIA', 'A03', 'P01', 'ENTRY'),
        ('MFR-DEMO-BOLO', 'PADARIA', 'A03', 'P02', 'FAR'),
        ('MFR-DEMO-AGUA', 'BEBIDAS', 'A05', 'BE01', 'ENTRY'),
        ('MFR-DEMO-SUCO', 'BEBIDAS', 'A05', 'BE02', 'FAR'),
        ('MFR-DEMO-DETERGENTE', 'LIMPEZA', 'A07', 'HZ01', 'ENTRY'),
        ('MFR-DEMO-SABAO', 'LIMPEZA', 'A07', 'HZ02', 'FAR'),
        ('MFR-DEMO-ARROZ', 'MERCEARIA', 'A09', 'M01', 'ENTRY'),
        ('MFR-DEMO-FEIJAO', 'MERCEARIA', 'A09', 'M02', 'FAR'),
        ('MFR-DEMO-LEITE', 'LATICINIOS', 'A11', 'LT01', 'ENTRY'),
        ('MFR-DEMO-QUEIJO', 'LATICINIOS', 'A11', 'LT02', 'FAR'),
        ('MFR-DEMO-ERVILHA', 'CONGELADOS', 'A13', 'C01', 'ENTRY'),
        ('MFR-DEMO-SORVETE', 'CONGELADOS', 'A13', 'C02', 'FAR')
)
INSERT INTO product_location (
    id, store_id, store_product_id, map_id, sector_id, aisle_id, shelf_block_id,
    module, shelf_level, x, y, navigation_node_id, primary_location, active
)
SELECT md5('marketfastroute.demo.v3.location.' || demo.sku)::uuid,
       map_ref.store_id,
       store_product.id,
       map_ref.map_id,
       sector.id,
       aisle.id,
       shelf_block.id,
       'Módulo 1',
       1,
       node.x,
       node.y,
       md5('marketfastroute.demo.v3.node.' || node.node_code)::uuid,
       TRUE,
       TRUE
FROM demo_target_map map_ref
CROSS JOIN demo_locations demo
JOIN store_product ON store_product.store_id = map_ref.store_id
JOIN product ON product.id = store_product.product_id AND product.sku = demo.sku
JOIN sector ON sector.map_id = map_ref.map_id AND sector.code = demo.sector_code
JOIN aisle ON aisle.map_id = map_ref.map_id AND aisle.code = demo.aisle_code
JOIN shelf_block ON shelf_block.map_id = map_ref.map_id AND shelf_block.code = demo.shelf_code
JOIN demo_nodes node ON node.node_code = 'AISLE-' || demo.aisle_code || '-' || demo.stop_point
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
    primary_location = EXCLUDED.primary_location,
    active = TRUE;

COMMIT;
