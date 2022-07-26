alter table tls_scan_result add column leaf_certificate VARCHAR(256)   references certificate;
alter table tls_scan_result add column certificate_expired         BOOLEAN;
alter table tls_scan_result add column certificate_too_soon        BOOLEAN;
alter table tls_scan_result add chain_trusted_by_java_platform     BOOLEAN;

alter table scan_result drop column leaf_certificate;
alter table scan_result drop column certificate_expired;
alter table scan_result drop column certificate_too_soon;
alter table scan_result drop column chain_trusted_by_java_platform;

