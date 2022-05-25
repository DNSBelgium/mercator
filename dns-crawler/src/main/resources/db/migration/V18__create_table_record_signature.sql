CREATE TABLE record_signature (
    id              SERIAL PRIMARY KEY,
    key_tag         INT NOT NULL, --(footprint)
    algorithm       INT NOT NULL,
    labels          INT NOT NULL,
    ttl             INT NOT NULL,
    inception_date  TIMESTAMP NOT NULL,
    expiration_date TIMESTAMP NOT NULL,
    signer          VARCHAR(255) NOT NULL,
    request_id      INT
);

CREATE INDEX ON record_signature (request_id);

ALTER TABLE record_signature
    ADD CONSTRAINT dns_record_signature_request_id_fk FOREIGN KEY (request_id) REFERENCES request(id);