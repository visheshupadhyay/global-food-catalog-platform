ALTER TABLE restaurant_chain
    RENAME COLUMN name TO display_name;

ALTER TABLE restaurant_chain
    ADD COLUMN business_code VARCHAR(100),
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;


UPDATE restaurant_chain
SET business_code = 'CHAIN-' || id
WHERE business_code IS NULL;

ALTER TABLE restaurant_chain
    ALTER COLUMN business_code SET NOT NULL,
    ADD CONSTRAINT restaurant_chain_business_code_uk UNIQUE (business_code),
    ADD CONSTRAINT restaurant_chain_status_ck
        CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'));

ALTER TABLE menu_group
    RENAME COLUMN name TO display_name;

ALTER TABLE menu_group
    ADD CONSTRAINT menu_group_status_ck
        CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'));


ALTER TABLE outlet
    RENAME COLUMN name TO display_name;

ALTER TABLE outlet
    ADD COLUMN outlet_code VARCHAR(100),
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- V1 has no outlet code, so preserve existing rows with a deterministic code.
UPDATE outlet
SET outlet_code = 'OUTLET-' || id
WHERE outlet_code IS NULL;

ALTER TABLE outlet
    ALTER COLUMN outlet_code SET NOT NULL,
    ADD CONSTRAINT outlet_restaurant_chain_id_outlet_code_uk
        UNIQUE (restaurant_chain_id, outlet_code),
    ADD CONSTRAINT outlet_status_ck
        CHECK (status IN ('DRAFT', 'ACTIVE', 'ARCHIVED'));
