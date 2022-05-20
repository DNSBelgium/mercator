CREATE TABLE record_signature (
    id              SERIAL PRIMARY KEY,
    key_tag         INT NOT NULL, --(footprint)
    algorithm       INT NOT NULL,
    labels          INT NOT NULL,
    ttl             INT NOT NULL,
    inception_date  TIMESTAMP NOT NULL,
    expiration_date TIMESTAMP NOT NULL,
    signer          VARCHAR(255) NOT NULL,
    request_id      INT NOT NULL
);