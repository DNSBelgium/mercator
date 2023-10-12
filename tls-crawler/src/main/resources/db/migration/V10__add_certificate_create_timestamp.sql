ALTER TABLE certificate
    ADD COLUMN create_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

UPDATE certificate
SET create_timestamp = '1970-01-01'::timestamp;
