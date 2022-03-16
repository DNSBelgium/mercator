CREATE TABLE dispatcher_event
(
    visit_id        UUID PRIMARY KEY,
    domain_name     VARCHAR(128) NOT NULL
);

CREATE TABLE dispatcher_event_labels
(
    visit_id        UUID         NOT NULL REFERENCES dispatcher_event(visit_id),
    labels          VARCHAR(128) NOT NULL
);