CREATE TABLE record_signature (
    id              SERIAL PRIMARY KEY,
    key_tag         INT, --(footprint)
    algorithm       INT,
    labels          INT,
    ttl             INT,
    inception_date  TIMESTAMP,
    expiration_date TIMESTAMP,
    signer          VARCHAR(255),
    request_id      INT
);

CREATE INDEX ON record_signature (request_id);

ALTER TABLE record_signature
    ADD CONSTRAINT dns_record_signature_request_id_fk FOREIGN KEY (request_id) REFERENCES request(id);