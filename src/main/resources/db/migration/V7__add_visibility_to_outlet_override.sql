ALTER TABLE outlet_override
    ADD COLUMN override_visible BOOLEAN;

ALTER TABLE outlet_override
    DROP CONSTRAINT outlet_override_sparse_value_ck;

ALTER TABLE outlet_override
    ADD CONSTRAINT outlet_override_sparse_value_ck
        CHECK (override_item_name IS NOT NULL
            OR override_price IS NOT NULL
            OR override_available IS NOT NULL
            OR override_visible IS NOT NULL);
