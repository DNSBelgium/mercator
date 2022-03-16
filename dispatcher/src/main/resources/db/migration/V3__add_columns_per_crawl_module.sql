CREATE TABLE dispatcher_event_acks (
    visit_id        UUID            NOT NULL REFERENCES dispatcher_event(visit_id),
    acks            TIMESTAMP WITH TIME ZONE,
    acks_key        VARCHAR(255)    NOT NULL,
    primary key (visit_id, acks_key)
);