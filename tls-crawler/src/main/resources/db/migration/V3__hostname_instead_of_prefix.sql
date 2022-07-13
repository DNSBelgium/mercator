alter table tls_scan_result add host_name VARCHAR(128);

update tls_scan_result set host_name = case when prefix is null then domain_name else prefix || '.' || domain_name end;
alter table tls_scan_result alter column host_name set not null;

create unique index tls_scan_result_visitid_hostname_index on tls_scan_result(visit_id, host_name);

alter table tls_scan_result drop column prefix;

