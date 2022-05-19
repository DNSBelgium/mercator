ALTER TABLE request
    ADD COLUMN IF NOT EXISTS num_of_responses INT;

UPDATE request r
    SET num_of_responses = (SELECT count(1) FROM response WHERE request_id = r.id)
    WHERE r.num_of_responses IS NULL;

ALTER TABLE request
    ALTER COLUMN num_of_responses SET NOT NULL;