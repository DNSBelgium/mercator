alter table html_features add column fraction_words_in_dn float;
alter table html_features add column fraction_words_in_url float;
alter table html_features add column nb_distinct_words_in_title integer;
alter table html_features add column distance_dn_title integer;
alter table html_features add column substring_dn_title integer;
