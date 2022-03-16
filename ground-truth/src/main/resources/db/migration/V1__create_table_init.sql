create table labeling_context (
    id          SERIAL          PRIMARY KEY,
    name        VARCHAR(128)    NOT NULL,
    version     INTEGER         NOT NULL,
    description TEXT
);

create table labels (
    id                  SERIAL          PRIMARY KEY,
    labeling_context_id integer         REFERENCES labeling_context(id) NOT NULL,
    label               VARCHAR(128)                                    NOT NULL
);

create table labeled_visits (
    visit_id    UUID,
    label       INTEGER                 REFERENCES labels(id),
    username    VARCHAR,
    timestamp   TIMESTAMP WITH TIME ZONE                        NOT NULL,
    PRIMARY KEY (visit_id, label)
);
