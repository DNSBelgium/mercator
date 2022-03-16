alter table html_features alter column title type varchar(2000) using title::varchar(2000);
alter table html_features alter column htmlstruct type varchar(2000) using htmlstruct::varchar(2000);
