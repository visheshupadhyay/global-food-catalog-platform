ALTER TABLE menu_item
    ADD COLUMN business_code VARCHAR(100),
    ADD COLUMN status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    ADD COLUMN visibility BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN availability BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN restaurant_chain_id BIGINT,
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

-- Keep the chain ID beside the item so codes can be unique within one chain.
UPDATE menu_item item
SET restaurant_chain_id = menu_group.restaurant_chain_id
FROM menu_group
WHERE item.menu_group_id = menu_group.id;

UPDATE menu_item item
SET restaurant_chain_id = outlet.restaurant_chain_id
FROM outlet
WHERE item.outlet_id = outlet.id;

UPDATE menu_item
SET business_code = 'CHAIN-' || id
WHERE business_code IS NULL;

ALTER TABLE menu_item
    ALTER COLUMN business_code SET NOT NULL,
    ALTER COLUMN restaurant_chain_id SET NOT NULL,
    ADD CONSTRAINT menu_item_status_ck
        CHECK (status IN ('DRAFT', 'ACTIVE', 'HIDDEN', 'ARCHIVED')),
    ADD CONSTRAINT menu_item_version_non_negative_ck
        CHECK (version >= 0),
    ADD CONSTRAINT menu_item_business_code_chain_uk
        UNIQUE (restaurant_chain_id, business_code);

ALTER TABLE menu_group
    ADD CONSTRAINT menu_group_id_chain_uk
        UNIQUE (id, restaurant_chain_id);

ALTER TABLE outlet
    ADD CONSTRAINT outlet_id_chain_uk
        UNIQUE (id, restaurant_chain_id);

ALTER TABLE menu_item
    ADD CONSTRAINT menu_item_menu_group_chain_fk
        FOREIGN KEY (menu_group_id, restaurant_chain_id)
        REFERENCES menu_group (id, restaurant_chain_id),
    ADD CONSTRAINT menu_item_outlet_chain_fk
        FOREIGN KEY (outlet_id, restaurant_chain_id)
        REFERENCES outlet (id, restaurant_chain_id);
