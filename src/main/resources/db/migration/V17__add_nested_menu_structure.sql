-- Existing menu items are atomic products unless explicitly created as a bundle.
ALTER TABLE menu_item
    ADD COLUMN menu_item_kind VARCHAR(20) NOT NULL DEFAULT 'PRODUCT',
    ADD CONSTRAINT menu_item_kind_ck CHECK (menu_item_kind IN ('PRODUCT', 'BUNDLE'));

-- A bundle is a menu item whose ordered children are products or other bundles.
CREATE TABLE bundle_child (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    bundle_menu_item_id BIGINT NOT NULL REFERENCES menu_item (id),
    child_menu_item_id BIGINT NOT NULL REFERENCES menu_item (id),
    quantity INTEGER NOT NULL,
    display_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT bundle_child_quantity_ck CHECK (quantity > 0),
    CONSTRAINT bundle_child_display_order_ck CHECK (display_order >= 0),
    CONSTRAINT bundle_child_no_direct_self_reference_ck
        CHECK (bundle_menu_item_id <> child_menu_item_id),
    CONSTRAINT bundle_child_bundle_child_uk
        UNIQUE (bundle_menu_item_id, child_menu_item_id),
    CONSTRAINT bundle_child_bundle_display_order_uk
        UNIQUE (bundle_menu_item_id, display_order)
);

CREATE INDEX bundle_child_bundle_menu_item_id_idx
    ON bundle_child (bundle_menu_item_id);
CREATE INDEX bundle_child_child_menu_item_id_idx
    ON bundle_child (child_menu_item_id);

-- A product or bundle owns ordered groups of selectable atomic options.
CREATE TABLE modifier_group (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    owner_menu_item_id BIGINT NOT NULL REFERENCES menu_item (id),
    group_name VARCHAR(100) NOT NULL,
    minimum_selections INTEGER NOT NULL,
    maximum_selections INTEGER NOT NULL,
    display_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT modifier_group_selection_bounds_ck
        CHECK (minimum_selections >= 0 AND maximum_selections >= minimum_selections),
    CONSTRAINT modifier_group_display_order_ck CHECK (display_order >= 0),
    CONSTRAINT modifier_group_owner_display_order_uk
        UNIQUE (owner_menu_item_id, display_order)
);

CREATE INDEX modifier_group_owner_menu_item_id_idx
    ON modifier_group (owner_menu_item_id);

-- Options are atomic add-ons; a zero price delta means no additional charge.
CREATE TABLE modifier_option (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    modifier_group_id BIGINT NOT NULL REFERENCES modifier_group (id),
    option_name VARCHAR(100) NOT NULL,
    price_delta NUMERIC(12, 2) NOT NULL DEFAULT 0,
    display_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT modifier_option_price_delta_ck CHECK (price_delta >= 0),
    CONSTRAINT modifier_option_display_order_ck CHECK (display_order >= 0),
    CONSTRAINT modifier_option_group_display_order_uk
        UNIQUE (modifier_group_id, display_order)
);

CREATE INDEX modifier_option_modifier_group_id_idx
    ON modifier_option (modifier_group_id);
