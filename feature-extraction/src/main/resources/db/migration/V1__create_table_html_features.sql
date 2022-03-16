CREATE TABLE html_features
(
    id                            SERIAL PRIMARY KEY,
    visit_id                      UUID                        NOT NULL,
    crawl_timestamp               timestamp with time zone    NOT NULL,
    domain_name                   VARCHAR(128)                NOT NULL,
    nb_imgs                       INTEGER,
    nb_links_int                  INTEGER,
    nb_links_ext                  INTEGER,
    nb_links_tel                  INTEGER,
    nb_links_email                INTEGER,
    nb_input_txt                  INTEGER,
    nb_button                     INTEGER,
    nb_meta_desc                  INTEGER,
    nb_meta_keyw                  INTEGER,
    nb_numerical_strings          INTEGER,
    nb_tags                       INTEGER,
    nb_words                      INTEGER,
    title                         VARCHAR(500),
    htmlstruct                    VARCHAR(500),
    body_text                     TEXT,
    meta_text                     TEXT
);

