with typed as (select *
  from read_json(coalesce(getvariable('jsonLocation'), '~/mercator/json/dns/*.json'), columns ={
        requests: 'struct(
            domain_name VARCHAR,
            prefix VARCHAR,
            record_type VARCHAR,
            rcode BIGINT,
            crawl_started TIMESTAMP,
            crawl_finished TIMESTAMP,
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
        crawl_started: 'TIMESTAMP',
        crawl_finished: 'TIMESTAMP',
        visit_id: 'VARCHAR' }
    )
),
     added_year_month as (
         select
             *,
             string_to_array(domain_name, '.')[-1] as tld,
             year(crawl_started) as year,
             month(crawl_started) as month
from typed
    )