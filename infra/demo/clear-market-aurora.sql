BEGIN;

DO $$
DECLARE
    demo_store_id UUID := md5('marketfastroute.demo.store')::uuid;
    demo_product_ids UUID[] := ARRAY(SELECT id FROM product WHERE sku LIKE 'MFR-DEMO-%');
    demo_category_ids UUID[] := ARRAY(SELECT id FROM category WHERE code LIKE 'MFR-DEMO-%');
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
        WHERE sku LIKE 'MFR-DEMO-%'
          AND id <> md5('marketfastroute.demo.product.' || sku)::uuid
    ) THEN
        RAISE EXCEPTION 'Refusing to clear products outside the reserved demo namespace';
    END IF;

    IF EXISTS (
        SELECT 1 FROM category
        WHERE code LIKE 'MFR-DEMO-%'
          AND id <> md5('marketfastroute.demo.category.' || code)::uuid
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
WHERE sku LIKE 'MFR-DEMO-%';

DELETE FROM category
WHERE code LIKE 'MFR-DEMO-%';

COMMIT;
