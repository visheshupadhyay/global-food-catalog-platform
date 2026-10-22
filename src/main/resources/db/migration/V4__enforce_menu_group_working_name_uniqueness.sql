ALTER TABLE menu_group
    ADD COLUMN normalized_display_name VARCHAR(100)
    GENERATED ALWAYS AS (LOWER(BTRIM(display_name))) STORED;

CREATE UNIQUE INDEX menu_group_normalized_display_name_uk
    ON menu_group (restaurant_chain_id, normalized_display_name)
    WHERE status IN ('DRAFT', 'ACTIVE');