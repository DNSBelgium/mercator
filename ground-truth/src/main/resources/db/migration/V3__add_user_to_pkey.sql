alter table ground_truth.labeled_visits drop constraint labeled_visits_pkey;
alter table ground_truth.labeled_visits add primary key (visit_id, username, label);
