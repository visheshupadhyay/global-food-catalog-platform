CREATE TABLE menu_item_seasonal_availability (
    id BIGSERIAL PRIMARY KEY,
    menu_item_id BIGINT NOT NULL REFERENCES menu_item (id),
    outlet_id BIGINT NULL REFERENCES outlet (id),
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    season_version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT menu_item_seasonal_availability_dates_ck CHECK (start_date < end_date),
    CONSTRAINT menu_item_seasonal_availability_version_ck CHECK (season_version >= 0)
);

CREATE UNIQUE INDEX menu_item_seasonal_availability_base_uk
    ON menu_item_seasonal_availability (menu_item_id) WHERE outlet_id IS NULL;
CREATE UNIQUE INDEX menu_item_seasonal_availability_outlet_uk
    ON menu_item_seasonal_availability (menu_item_id, outlet_id) WHERE outlet_id IS NOT NULL;
