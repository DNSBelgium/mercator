create index dispatcher_event_labels_labels_index
    on dispatcher_event_labels (labels);

create index dispatcher_event_domainname_index
    on dispatcher_event (domain_name);
