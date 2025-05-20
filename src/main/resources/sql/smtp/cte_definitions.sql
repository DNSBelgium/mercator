with typed as (
from read_json(
  coalesce(getvariable('jsonLocation'), '~/mercator/json/smtp/*.json'),
  columns = {
    visit_id: 'VARCHAR',
    crawl_status: 'VARCHAR',
    domain_name: 'VARCHAR',
    timestamp: 'TIMESTAMP',
    num_conversations: 'BIGINT',
    hosts: 'struct(
      from_mx BOOLEAN,
      host_name VARCHAR,
      priority BIGINT,
      conversations struct(
        ip VARCHAR,
        asn BIGINT,
        country VARCHAR,
        asn_organisation VARCHAR,
        banner VARCHAR,
        connect_ok BOOLEAN,
        connect_reply_code BIGINT,
        supported_extensions VARCHAR[],
        ip_version BIGINT,
        start_tls_ok BOOLEAN,
        start_tls_reply_code BIGINT,
        error_message VARCHAR,
        error VARCHAR,
        connection_time_ms BIGINT,
        software VARCHAR,
        software_version VARCHAR,
        timestamp TIMESTAMP
      )[]
    )[]'
  }
)
),
added_year_month as (
    select *, year(timestamp) as year, month(timestamp) as month
    from typed
)