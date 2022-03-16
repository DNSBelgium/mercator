alter table html_features drop column languages;
alter table html_features add column body_text_language varchar(128);
alter table html_features add column body_text_language_2 varchar(128);