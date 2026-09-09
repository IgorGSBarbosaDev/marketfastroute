CREATE TABLE store (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR NOT NULL,
    code VARCHAR NOT NULL,
    address VARCHAR NOT NULL,
    city VARCHAR NOT NULL,
    state VARCHAR NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_store_code UNIQUE (code)
);

CREATE TABLE category (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    parent_id UUID,
    name VARCHAR NOT NULL,
    code VARCHAR NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_category_code UNIQUE (code),
    CONSTRAINT ck_category_parent_not_self CHECK (parent_id IS NULL OR parent_id <> id),
    CONSTRAINT fk_category_parent
        FOREIGN KEY (parent_id) REFERENCES category (id) ON DELETE RESTRICT
);

CREATE TABLE product (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id UUID NOT NULL,
    sku VARCHAR NOT NULL,
    ean VARCHAR,
    name VARCHAR NOT NULL,
    brand VARCHAR,
    description TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_product_sku UNIQUE (sku),
    CONSTRAINT fk_product_category
        FOREIGN KEY (category_id) REFERENCES category (id) ON DELETE RESTRICT
);

CREATE UNIQUE INDEX ux_product_ean_not_null
    ON product (ean)
    WHERE ean IS NOT NULL;

CREATE TABLE store_product (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    store_id UUID NOT NULL,
    product_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    CONSTRAINT uq_store_product_store_product UNIQUE (store_id, product_id),
    CONSTRAINT uq_store_product_store_id UNIQUE (store_id, id),
    CONSTRAINT fk_store_product_store
        FOREIGN KEY (store_id) REFERENCES store (id) ON DELETE CASCADE,
    CONSTRAINT fk_store_product_product
        FOREIGN KEY (product_id) REFERENCES product (id) ON DELETE RESTRICT
);
