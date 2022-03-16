alter table dns_crawl_result_geo_ips
    add column asn_organisation VARCHAR(128);

-- https://dev.maxmind.com/geoip/geoip2/geolite2-asn-csv-database/
-- autonomous_system_organization
-- The organization associated with the registered autonomous system number for the IP address.

