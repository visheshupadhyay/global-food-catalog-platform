CREATE TABLE menu_item_schedule (
    id BIGSERIAL PRIMARY KEY,
    menu_item_id BIGINT NOT NULL,
    outlet_id BIGINT NULL,
    weekday VARCHAR(9) NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT menu_item_schedule_menu_item_fk
        FOREIGN KEY (menu_item_id) REFERENCES menu_item (id),
    CONSTRAINT menu_item_schedule_outlet_fk
        FOREIGN KEY (outlet_id) REFERENCES outlet (id),
    CONSTRAINT menu_item_schedule_weekday_ck
        CHECK (weekday IN ('MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY',
                           'FRIDAY', 'SATURDAY', 'SUNDAY')),
    CONSTRAINT menu_item_schedule_time_order_ck
        CHECK (start_time < end_time),
    CONSTRAINT menu_item_schedule_version_ck
        CHECK (version >= 0)
);

CREATE INDEX menu_item_schedule_item_weekday_idx
    ON menu_item_schedule (menu_item_id, weekday);

CREATE INDEX menu_item_schedule_outlet_weekday_idx
    ON menu_item_schedule (menu_item_id, outlet_id, weekday);

CREATE UNIQUE INDEX menu_item_schedule_base_window_uk
    ON menu_item_schedule (menu_item_id, weekday, start_time, end_time)
    WHERE outlet_id IS NULL;

CREATE UNIQUE INDEX menu_item_schedule_outlet_window_uk
    ON menu_item_schedule (menu_item_id, outlet_id, weekday, start_time, end_time)
    WHERE outlet_id IS NOT NULL;
