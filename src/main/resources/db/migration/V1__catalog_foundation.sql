-- One food brand, such as "Burger House", that owns many menus and outlets.
CREATE TABLE restaurant_chain (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL
);

-- A shared menu version for a chain, such as "Germany Spring Menu".
CREATE TABLE menu_group (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    restaurant_chain_id BIGINT NOT NULL,

    CONSTRAINT menu_group_restaurant_chain_id_fk
        FOREIGN KEY (restaurant_chain_id)
        REFERENCES restaurant_chain(id)
);

-- One physical restaurant location, such as "Burger House Berlin Mitte".
CREATE TABLE outlet (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    restaurant_chain_id BIGINT NOT NULL,

    CONSTRAINT outlet_restaurant_chain_id_fk
        FOREIGN KEY (restaurant_chain_id)
        REFERENCES restaurant_chain(id)
);

-- A customer-facing section; it belongs either to a shared menu group or one outlet.
-- Examples: shared "Burgers", or Berlin-only "Halloween Specials".
CREATE TABLE menu_category (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    menu_group_id BIGINT,
    outlet_id BIGINT,

    CONSTRAINT menu_category_menu_group_id_fk
        FOREIGN KEY (menu_group_id)
        REFERENCES menu_group(id),

    CONSTRAINT menu_category_outlet_id_fk
        FOREIGN KEY (outlet_id)
        REFERENCES outlet(id),

    CONSTRAINT menu_category_single_owner_ck
        CHECK ((menu_group_id IS NOT NULL AND outlet_id IS NULL)
            OR (menu_group_id IS NULL AND outlet_id IS NOT NULL))
);

-- A food item; it is either shared by a menu group or owned locally by one outlet.
-- Example: shared "Classic Burger", or Berlin-only "Berlin Curry Burger".
CREATE TABLE menu_item (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    item_name VARCHAR(100) NOT NULL,
    item_price NUMERIC(12,2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    menu_group_id BIGINT,
    menu_category_id BIGINT,
    outlet_id BIGINT,

    CONSTRAINT menu_item_menu_group_id_fk
        FOREIGN KEY (menu_group_id)
        REFERENCES menu_group(id),

    CONSTRAINT menu_item_menu_category_id_fk
        FOREIGN KEY (menu_category_id)
        REFERENCES menu_category(id),


    CONSTRAINT menu_item_outlet_id_fk
        FOREIGN KEY (outlet_id)
        REFERENCES outlet(id),

    CONSTRAINT menu_item_single_owner_ck
        CHECK ((menu_group_id IS NOT NULL AND outlet_id IS NULL)
            OR (menu_group_id IS NULL AND outlet_id IS NOT NULL)),

    CONSTRAINT menu_item_non_negative_price_ck
        CHECK (item_price >= 0)
);


-- Connects an outlet to the menu group it currently uses to build its menu.
-- Example: Berlin Mitte uses the Germany Spring Menu.
CREATE TABLE menu_group_outlet (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    menu_group_id BIGINT NOT NULL,
    outlet_id BIGINT NOT NULL,

    CONSTRAINT menu_group_outlet_menu_group_id_fk
        FOREIGN KEY (menu_group_id)
        REFERENCES menu_group(id),

    CONSTRAINT menu_group_outlet_outlet_id_fk
        FOREIGN KEY (outlet_id)
        REFERENCES outlet(id),

    CONSTRAINT menu_group_outlet_one_current_menu_per_outlet_uk
        UNIQUE (outlet_id)
);

-- Stores only fields an outlet changes from an inherited base item; null means "use the base value".
-- Example: Berlin changes Classic Burger price to 8.50 but keeps its base name.
CREATE TABLE outlet_override (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    outlet_id BIGINT NOT NULL,
    base_menu_item_id BIGINT NOT NULL,
    override_item_name VARCHAR(100),
    override_price NUMERIC(12,2),
    override_available BOOLEAN,

    CONSTRAINT outlet_override_outlet_id_fk
            FOREIGN KEY (outlet_id)
            REFERENCES outlet(id),

    CONSTRAINT outlet_override_base_item_id_fk
            FOREIGN KEY (base_menu_item_id)
            REFERENCES menu_item(id),

    CONSTRAINT outlet_override_price_ck
            CHECK (override_price IS NULL OR override_price >= 0),

    CONSTRAINT outlet_override_sparse_value_ck
            CHECK (override_item_name IS NOT NULL
                OR override_price IS NOT NULL
                OR override_available IS NOT NULL),

    CONSTRAINT outlet_override_unique_item_ck
            UNIQUE (outlet_id, base_menu_item_id)
);




-- Permanent history of catalog changes for investigation, support, and compliance.
-- Example: records that Berlin changed a burger price or hid an item.
CREATE TABLE catalog_audit (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    menu_group_id BIGINT,
    outlet_id BIGINT,

    CONSTRAINT catalog_audit_menu_group_id_fk
        FOREIGN KEY (menu_group_id)
        REFERENCES menu_group(id),

    CONSTRAINT catalog_audit_outlet_id_fk
        FOREIGN KEY (outlet_id)
        REFERENCES outlet(id)
);

-- Reliable event queue used to publish catalog changes to Kafka after a database transaction commits.
-- Example: a MenuItemPriceChanged event waiting for the event publisher.
CREATE TABLE outbox_event (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    menu_group_id BIGINT,
    outlet_id BIGINT,

    CONSTRAINT outbox_event_menu_group_id_fk
        FOREIGN KEY (menu_group_id)
        REFERENCES menu_group(id),

    CONSTRAINT outbox_event_outlet_id_fk
        FOREIGN KEY (outlet_id)
        REFERENCES outlet(id)
);
