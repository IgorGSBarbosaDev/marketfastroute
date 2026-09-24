BEGIN;

DO $$
DECLARE
    demo_store_id UUID := md5('marketfastroute.demo.store')::uuid;
    demo_product_ids UUID[] := ARRAY[
        md5('marketfastroute.demo.product.MFR-DEMO-BANANA')::uuid,
        md5('marketfastroute.demo.product.MFR-DEMO-MACA')::uuid,
        md5('marketfastroute.demo.product.MFR-DEMO-PAO')::uuid,
        md5('marketfastroute.demo.product.MFR-DEMO-BOLO')::uuid,
        md5('marketfastroute.demo.product.MFR-DEMO-AGUA')::uuid,
        md5('marketfastroute.demo.product.MFR-DEMO-SUCO')::uuid,
        md5('marketfastroute.demo.product.MFR-DEMO-LEITE')::uuid,
        md5('marketfastroute.demo.product.MFR-DEMO-QUEIJO')::uuid,
        md5('marketfastroute.demo.product.MFR-DEMO-ARROZ')::uuid,
        md5('marketfastroute.demo.product.MFR-DEMO-FEIJAO')::uuid,
        md5('marketfastroute.demo.product.MFR-DEMO-DETERGENTE')::uuid,
        md5('marketfastroute.demo.product.MFR-DEMO-SABAO')::uuid
    ];
    demo_category_ids UUID[] := ARRAY[
        md5('marketfastroute.demo.category.MFR-DEMO-HORTIFRUTI')::uuid,
        md5('marketfastroute.demo.category.MFR-DEMO-PADARIA')::uuid,
        md5('marketfastroute.demo.category.MFR-DEMO-BEBIDAS')::uuid,
        md5('marketfastroute.demo.category.MFR-DEMO-LATICINIOS')::uuid,
        md5('marketfastroute.demo.category.MFR-DEMO-MERCEARIA')::uuid,
        md5('marketfastroute.demo.category.MFR-DEMO-LIMPEZA')::uuid
    ];
BEGIN
    IF EXISTS (
        SELECT 1
        FROM store
        WHERE (code = 'MFR-DEMO-AURORA' AND id <> demo_store_id)
           OR (id = demo_store_id AND code <> 'MFR-DEMO-AURORA')
    ) THEN
        RAISE EXCEPTION 'Refusing to clear a store code or ID owned by another record';
    END IF;

    IF EXISTS (
        SELECT 1 FROM product
        WHERE sku LIKE 'MFR-DEMO-%' AND id <> ALL(demo_product_ids)
    ) OR EXISTS (
        SELECT 1 FROM product
        WHERE id = ANY(demo_product_ids)
          AND sku NOT IN (
            'MFR-DEMO-BANANA', 'MFR-DEMO-MACA', 'MFR-DEMO-PAO', 'MFR-DEMO-BOLO',
            'MFR-DEMO-AGUA', 'MFR-DEMO-SUCO', 'MFR-DEMO-LEITE', 'MFR-DEMO-QUEIJO',
            'MFR-DEMO-ARROZ', 'MFR-DEMO-FEIJAO', 'MFR-DEMO-DETERGENTE', 'MFR-DEMO-SABAO'
          )
    ) THEN
        RAISE EXCEPTION 'Refusing to clear products outside the reserved demo namespace';
    END IF;

    IF EXISTS (
        SELECT 1 FROM category
        WHERE code LIKE 'MFR-DEMO-%' AND id <> ALL(demo_category_ids)
    ) OR EXISTS (
        SELECT 1 FROM category
        WHERE id = ANY(demo_category_ids)
          AND code NOT IN (
            'MFR-DEMO-HORTIFRUTI', 'MFR-DEMO-PADARIA', 'MFR-DEMO-BEBIDAS',
            'MFR-DEMO-LATICINIOS', 'MFR-DEMO-MERCEARIA', 'MFR-DEMO-LIMPEZA'
          )
    ) THEN
        RAISE EXCEPTION 'Refusing to clear categories outside the reserved demo namespace';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM store_product store_product
        JOIN product product ON product.id = store_product.product_id
        WHERE (product.sku LIKE 'MFR-DEMO-%' OR product.id = ANY(demo_product_ids))
          AND store_product.store_id <> demo_store_id
    ) THEN
        RAISE EXCEPTION 'Refusing to clear demo products linked to another store';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM product product
        JOIN category category ON category.id = product.category_id
        WHERE (category.code LIKE 'MFR-DEMO-%' OR category.id = ANY(demo_category_ids))
          AND product.sku NOT LIKE 'MFR-DEMO-%'
          AND product.id <> ALL(demo_product_ids)
    ) THEN
        RAISE EXCEPTION 'Refusing to clear demo categories used by another product';
    END IF;

    IF EXISTS (
        SELECT 1
        FROM category child
        JOIN category parent ON parent.id = child.parent_id
        WHERE (parent.code LIKE 'MFR-DEMO-%' OR parent.id = ANY(demo_category_ids))
          AND child.code NOT LIKE 'MFR-DEMO-%'
          AND child.id <> ALL(demo_category_ids)
    ) THEN
        RAISE EXCEPTION 'Refusing to clear demo categories used as a parent';
    END IF;
END;
$$;

DELETE FROM store
WHERE code = 'MFR-DEMO-AURORA'
   OR id = md5('marketfastroute.demo.store')::uuid;

DELETE FROM product
WHERE sku LIKE 'MFR-DEMO-%'
   OR id IN (
       md5('marketfastroute.demo.product.MFR-DEMO-BANANA')::uuid,
       md5('marketfastroute.demo.product.MFR-DEMO-MACA')::uuid,
       md5('marketfastroute.demo.product.MFR-DEMO-PAO')::uuid,
       md5('marketfastroute.demo.product.MFR-DEMO-BOLO')::uuid,
       md5('marketfastroute.demo.product.MFR-DEMO-AGUA')::uuid,
       md5('marketfastroute.demo.product.MFR-DEMO-SUCO')::uuid,
       md5('marketfastroute.demo.product.MFR-DEMO-LEITE')::uuid,
       md5('marketfastroute.demo.product.MFR-DEMO-QUEIJO')::uuid,
       md5('marketfastroute.demo.product.MFR-DEMO-ARROZ')::uuid,
       md5('marketfastroute.demo.product.MFR-DEMO-FEIJAO')::uuid,
       md5('marketfastroute.demo.product.MFR-DEMO-DETERGENTE')::uuid,
       md5('marketfastroute.demo.product.MFR-DEMO-SABAO')::uuid
   );

UPDATE category
SET parent_id = NULL
WHERE code LIKE 'MFR-DEMO-%'
   OR id IN (
       md5('marketfastroute.demo.category.MFR-DEMO-HORTIFRUTI')::uuid,
       md5('marketfastroute.demo.category.MFR-DEMO-PADARIA')::uuid,
       md5('marketfastroute.demo.category.MFR-DEMO-BEBIDAS')::uuid,
       md5('marketfastroute.demo.category.MFR-DEMO-LATICINIOS')::uuid,
       md5('marketfastroute.demo.category.MFR-DEMO-MERCEARIA')::uuid,
       md5('marketfastroute.demo.category.MFR-DEMO-LIMPEZA')::uuid
   );

DELETE FROM category
WHERE code LIKE 'MFR-DEMO-%'
   OR id IN (
       md5('marketfastroute.demo.category.MFR-DEMO-HORTIFRUTI')::uuid,
       md5('marketfastroute.demo.category.MFR-DEMO-PADARIA')::uuid,
       md5('marketfastroute.demo.category.MFR-DEMO-BEBIDAS')::uuid,
       md5('marketfastroute.demo.category.MFR-DEMO-LATICINIOS')::uuid,
       md5('marketfastroute.demo.category.MFR-DEMO-MERCEARIA')::uuid,
       md5('marketfastroute.demo.category.MFR-DEMO-LIMPEZA')::uuid
   );

COMMIT;
