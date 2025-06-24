with typed as (select *
  from read_json(coalesce(getvariable('jsonLocation'), '~/mercator/json/dns/*.json'), columns ={
        requests: 'struct(
            domain_name VARCHAR,
            prefix VARCHAR,
            record_type VARCHAR,
            rcode BIGINT,
            crawl_timestamp TIMESTAMP,
            ok BOOLEAN,
            problem VARCHAR,
            num_of_responses BIGINT,
            responses struct(
              record_data VARCHAR,
              ttl BIGINT,
              response_geo_ips struct(
                asn VARCHAR,
                country VARCHAR,
                ip VARCHAR,
                asn_organisation VARCHAR,
                ip_version BIGINT
              )[]
            )[]
          )[]',
        status: 'VARCHAR',
        domain_name: 'VARCHAR',
        crawl_timestamp: 'TIMESTAMP',
        visit_id: 'VARCHAR' }
    )
),
     added_year_month as (
         select *, year(crawl_timestamp) as year, month(crawl_timestamp) as month
from typed
    )