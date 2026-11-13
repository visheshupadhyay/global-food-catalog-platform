ALTER TABLE outlet
    ADD COLUMN timezone VARCHAR(64);

UPDATE outlet
SET timezone = 'UTC'
WHERE timezone IS NULL OR BTRIM(timezone) = '';

ALTER TABLE outlet
    ALTER COLUMN timezone SET DEFAULT 'UTC',
    ALTER COLUMN timezone SET NOT NULL;
