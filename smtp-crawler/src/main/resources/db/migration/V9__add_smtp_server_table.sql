CREATE TABLE smtp_server
(
    id        SERIAL PRIMARY KEY,
    host_name VARCHAR(128) NOT NULL,
    priority  int          NOT NULL
);