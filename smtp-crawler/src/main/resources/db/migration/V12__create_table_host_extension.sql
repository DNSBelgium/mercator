CREATE TABLE host_extension
(
    id           SERIAL PRIMARY KEY,
    host_id      int NOT NULL REFERENCES smtp_host,
    extension_id int NOT NULL REFERENCES extension
);