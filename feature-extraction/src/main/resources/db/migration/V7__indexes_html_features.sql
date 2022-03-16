-- Extra indexes to speed up some select statements

create index html_features_visit_id_url on html_features(visit_id, url);
create index html_features_domain_name on html_features(domain_name);
create index html_features_title on html_features(title);

