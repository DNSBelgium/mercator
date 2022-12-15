-- Store predictions from classifier inside html_features
-- This makes it MUCH faster to find the rows/visits that have not yet been classified.

alter table html_features add column proba_low_content float;
alter table html_features add column is_low_content boolean;

alter table html_features add column proba_fake_shop float;
alter table html_features add column is_fake_shop boolean;
