package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.f4b6a3.ulid.Ulid;
import lombok.SneakyThrows;
import org.apache.commons.text.StringSubstitutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class TlsRepository extends BaseRepository<TlsCrawlResult> {

  private static final Logger logger = LoggerFactory.getLogger(TlsRepository.class);

  private final String visitsLocation;

  private final String certificatesLocation;

  @SneakyThrows
  public TlsRepository(ObjectMapper objectMapper, @Value("${mercator.data.location:mercator/data/}tls") String dataLocation) {
    super(objectMapper, dataLocation, TlsCrawlResult.class);
    this.visitsLocation = createDestination(dataLocation, "visits");
    this.certificatesLocation = createDestination(dataLocation, "certificates");
  }

  @Override
  public String getAllItemsQuery() {
    return StringSubstitutor.replace("""
        with visits as (
            from read_parquet('${base_location}/**/*.parquet')
        ),
        nested as (
          select visit_id,
              domain_name,
              host_name,
              host_name_matches_certificate,
              chain_trusted_by_java_platform,
              certificate_expired,
              certificate_too_soon,
              certificate_chain_fingerprints,
              epoch(crawl_timestamp) as crawl_timestamp,
              {
                  crawl_timestamp: epoch(full_scan_crawl_timestamp),
                          ip: ip,
                          server_name: server_name,
                          connect_ok: connect_ok,
                          support_tls_1_2: support_tls_1_2,
                          support_tls_1_1: support_tls_1_1,
                          support_tls_1_0: support_tls_1_0,
                          support_ssl_3_0: support_ssl_3_0,
                          support_ssl_2_0: support_ssl_2_0,
                          selected_cipher_tls_1_3: selected_cipher_tls_1_3,
                          selected_cipher_tls_1_2: selected_cipher_tls_1_2,
                          selected_cipher_tls_1_1: selected_cipher_tls_1_1,
                          selected_cipher_tls_1_0: selected_cipher_tls_1_0,
                          selected_cipher_ssl_3_0: selected_cipher_ssl_3_0,
                          lowest_version_supported: lowest_version_supported,
                          highest_version_supported: highest_version_supported,
                          error_tls_1_3: error_tls_1_3,
                          error_tls_1_2: error_tls_1_2,
                          error_tls_1_1: error_tls_1_1,
                          error_tls_1_0: error_tls_1_0,
                          error_ssl_3_0: error_ssl_3_0,
                          error_ssl_2_0: error_ssl_2_0,
                          millis_tls_1_3: millis_tls_1_3,
                          millis_tls_1_2: millis_tls_1_2,
                          millis_tls_1_1: millis_tls_1_1,
                          millis_tls_1_0: millis_tls_1_0,
                          millis_ssl_3_0: millis_ssl_3_0,
                          millis_ssl_2_0: millis_ssl_2_0
              } as full_scan_entity
          from visits
        ),
        results as (
            select visit_id,
                   domain_name,
                   min(crawl_timestamp) as crawl_timestamp,
                   list(struct_pack(*columns(*))) as visits
            from nested
            group by 1, 2
        )
        select * from results
        
        """, Map.of("base_location", visitsLocation));
  }

  @Override
  public void storeResults(String jsonResultsLocation) {
    String baseQuery = StringSubstitutor.replace("""
        with tls as (
            from read_json('${location}')
        ),
        visits as (
            select unnest(visits, max_depth := 2)
            from tls
        ),
        visits_typed as (
            select visit_id::varchar as visit_id,
                domain_name::varchar as domain_name,
                host_name::varchar as host_name,
                host_name_matches_certificate::bool as host_name_matches_certificate,
                chain_trusted_by_java_platform::bool as chain_trusted_by_java_platform,
                certificate_expired::bool as certificate_expired,
                certificate_too_soon::bool as certificate_too_soon,
                certificate_chain_fingerprints::varchar[] as certificate_chain_fingerprints,
                to_timestamp(crawl_timestamp) as crawl_timestamp
            from visits
        ),
        full_scan_entity as (
            select unnest(full_scan_entity)
            from visits
        ),
        full_scan_typed as (
            select to_timestamp(crawl_timestamp) as full_scan_crawl_timestamp,
                ip::varchar as ip,
                server_name::varchar as server_name,
                connect_ok::bool as connect_ok,
                support_tls_1_2::bool as support_tls_1_2,
                support_tls_1_1::bool as support_tls_1_1,
                support_tls_1_0::bool as support_tls_1_0,
                support_ssl_3_0::bool as support_ssl_3_0,
                support_ssl_2_0::bool as support_ssl_2_0,
                selected_cipher_tls_1_3::varchar as selected_cipher_tls_1_3,
                selected_cipher_tls_1_2::varchar as selected_cipher_tls_1_2,
                selected_cipher_tls_1_1::varchar as selected_cipher_tls_1_1,
                selected_cipher_tls_1_0::varchar as selected_cipher_tls_1_0,
                selected_cipher_ssl_3_0::varchar as selected_cipher_ssl_3_0,
                lowest_version_supported::varchar as lowest_version_supported,
                highest_version_supported::varchar as highest_version_supported,
                error_tls_1_3::varchar as error_tls_1_3,
                error_tls_1_2::varchar as error_tls_1_2,
                error_tls_1_1::varchar as error_tls_1_1,
                error_tls_1_0::varchar as error_tls_1_0,
                error_ssl_3_0::varchar as error_ssl_3_0,
                error_ssl_2_0::varchar as error_ssl_2_0,
                millis_tls_1_3::int as millis_tls_1_3,
                millis_tls_1_2::int as millis_tls_1_2,
                millis_tls_1_1::int as millis_tls_1_1,
                millis_tls_1_0::int as millis_tls_1_0,
                millis_ssl_3_0::int as millis_ssl_3_0,
                millis_ssl_2_0::int as millis_ssl_2_0
            from full_scan_entity
        ),
        certificates_unnested as (
          select unnest(cast(certificate_chain as struct(
                      version int,
                      serial_number_hex varchar,
                      public_key_schema varchar,
                      public_key_length int,
                      not_before bigint,
                      not_after bigint,
                      issuer varchar,
                      subject varchar,
                      signature_hash_algorithm varchar,
                      sha256_fingerprint varchar,
                      subject_alternative_names varchar[]
          )[]), max_depth :=2 ) from visits
        ),
        certificates_typed as (
            select * exclude(not_before, not_after),
                   to_timestamp(not_before) as not_before,
                   to_timestamp(not_after) as not_after
            from certificates_unnested
        ),
        export_visits as (
            from visits_typed
            positional join
            full_scan_typed
        ),
        export_certificates as (
          select distinct(*) from certificates_typed
        )
        """, Map.of("location", jsonResultsLocation));
    this.getJdbcClient().sql(
        StringSubstitutor.replace("""
            copy (
              ${base_query} 
              select
                *,
                year(${timestamp_field}) as year,
                month(${timestamp_field}) as month
              from export_visits
            ) to '${visits_location}' (format parquet, partition_by (year, month), append)
            """, Map.of("base_query", baseQuery, "visits_location", visitsLocation, "timestamp_field", timestampField()))
    ).update();
    this.getJdbcClient().sql(
        String.format("""
            copy (
              %s
              select * from export_certificates
            ) to '%s/%s.parquet'
            """, baseQuery, certificatesLocation, UUID.randomUUID())
    ).update();
  }
}
