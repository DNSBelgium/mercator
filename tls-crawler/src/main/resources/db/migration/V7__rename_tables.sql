alter table scan_result rename to full_scan;
alter table tls_scan_result rename to crawl_result;

alter table crawl_result rename column scan_result to full_scan;

alter table crawl_result ALTER column full_scan set not null;
