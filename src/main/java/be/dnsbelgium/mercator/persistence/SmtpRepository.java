package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.smtp.dto.SmtpVisit;
import com.fasterxml.jackson.databind.ObjectMapper;


import lombok.SneakyThrows;
import org.apache.commons.text.StringSubstitutor;
import org.springframework.beans.factory.annotation.Value;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

import org.springframework.stereotype.Repository;

@Repository
public class SmtpRepository extends BaseRepository<SmtpVisit> {
    private static final Logger logger = LoggerFactory.getLogger(SmtpRepository.class);

    private final String smtpVisitDestination;
    private final String smtpHostDestination;

    @Override
    public String getAllItemsQuery() {
        return StringSubstitutor.replace("""
                WITH
                    visits_result AS (
                        SELECT * EXCLUDE (year, month)
                        FROM read_parquet('${smtpVisitDestination}/**/*/*.parquet', union_by_name=True)
                    ),
                    hosts_result AS (
                        SELECT * EXCLUDE (year, month, visit_id)
                        FROM read_parquet('${smtpHostDestination}/**/*/*.parquet', union_by_name=True)
                    ),
                    hosts_per_visit AS (
                        SELECT visit_id, LIST(hosts_result ORDER BY conversation_timestamp) AS hosts
                        FROM hosts_result
                        GROUP BY visit_id
                    ),
                    combined AS (
                        SELECT visits_result.*, COALESCE(hosts_per_visit.hosts, []) AS hosts
                        FROM visits_result
                                 LEFT JOIN hosts_per_visit ON visits_result.visit_id = hosts_per_visit.visit_id
                    ),
                    unnested AS ( -- max_depth := 3 because conversation is also un-nested in this step
                        SELECT *, UNNEST(hosts, max_depth := 3)
                        FROM combined
                    ),
                    conversation_struct as (
                        select *, struct_pack(
                            id := conversation_id,
                            ip := conversation_ip,
                            asn := conversation_asn,
                            country := conversation_country,
                            asn_organisation := conversation_asn_organisation,
                            banner := conversation_banner,
                            connect_ok := conversation_connect_ok,
                            connect_reply_code := conversation_connect_reply_code,
                            supported_extensions := conversation_supported_extensions,
                            ip_version := conversation_ip_version,
                            start_tls_ok := conversation_start_tls_ok,
                            start_tls_reply_code := conversation_start_tls_reply_code,
                            error_message := conversation_error_message,
                            error := conversation_error,
                            connection_time_ms := conversation_connection_time_ms,
                            software := conversation_software,
                            software_version := conversation_software_version,
                            "timestamp" := conversation_timestamp
                    ) as conversation
                        from unnested
                    ),
                    host_struct as (
                        select
                            *,
                            struct_pack(
                                id := id,
                                from_mx := from_mx,
                                host_name := host_name,
                                priority := priority,
                                conversation := conversation) as host
                        from conversation_struct
                    ),
                    smtp_visit as (
                        select visit_id, domain_name, timestamp, num_conversations, list(host) as hosts, crawl_status from host_struct group by all
                    )
                select * from smtp_visit
                """, Map.of(
                        "smtpVisitDestination", smtpVisitDestination,
                        "smtpHostDestination", smtpHostDestination
        ));
    }

    @SneakyThrows
    public SmtpRepository(ObjectMapper objectMapper, @Value("${mercator.data.location:mercator/data/}") String baseLocation) {
        super(objectMapper, baseLocation, SmtpVisit.class);
        String subPath = "smtp";
        smtpVisitDestination = createDestination(baseLocation, subPath, "visits");
        smtpHostDestination = createDestination(baseLocation, subPath, "hosts");
    }

    @Override
    public String timestampField(){
        return "timestamp";
    }

    @Override
    public void storeResults(String jsonResultsLocation) {
        String allResultsQuery = StringSubstitutor.replace("""
                select *,
                     year(to_timestamp(${timestampField})) as year,
                     month(to_timestamp(${timestampField})) as month
                from read_json('${jsonFile}')
                """, Map.of(
                        "timestampField", this.timestampField(),
                        "jsonFile", jsonResultsLocation
                ));
        logger.debug("AllresultsQuery = {}", allResultsQuery);

        String copySmtpVisitResult = StringSubstitutor.replace("""
                COPY (WITH all_results as (
                        ${allResultsQuery}
                      )
                      select
                            visit_id::VARCHAR as visit_id,
                            domain_name::VARCHAR as domain_name,
                            timestamp::DOUBLE as timestamp,
                            num_conversations::INTEGER as num_conversations,
                            crawl_status::VARCHAR as crawl_status,
                            year,
                            month
                      from all_results
                      )
                    to '${smtpVisitResultDestination}' (FORMAT parquet, PARTITION_BY (year, month), OVERWRITE_OR_IGNORE, FILENAME_PATTERN 'visits_{uuid}')
                """, Map.of(
                        "allResultsQuery", allResultsQuery,
                        "smtpVisitResultDestination", this.smtpVisitDestination
        ));
        logger.debug("copySmtpVisitResult = {}", copySmtpVisitResult);
        getJdbcClient().sql(copySmtpVisitResult).update();

        String copySmtpHosts = StringSubstitutor.replace("""
                COPY (
                      WITH all_results as (
                            ${allResultsQuery}
                          ),
                      extract_unnested_hosts_from_smtp AS (
                          SELECT visit_id, UNNEST(hosts, max_depth := 3), year, month
                          FROM all_results
                      ),
                      extract_hosts_and_conversation_from_smtp AS (
                          SELECT
                                visit_id        ::VARCHAR               as visit_id,
                                id              ::VARCHAR               as id,
                                from_mx         ::BOOLEAN               as from_mx,
                                host_name       ::VARCHAR               as host_name,
                                priority        ::INTEGER               as priority,
                                id_1            ::VARCHAR               as conversation_id,
                                ip::VARCHAR AS conversation_ip,
                                asn::BIGINT AS conversation_asn,
                                country::VARCHAR AS conversation_country,
                                asn_organisation::VARCHAR AS conversation_asn_organisation,
                                banner::VARCHAR AS conversation_banner,
                                connect_ok::BOOLEAN AS conversation_connect_ok,
                                connect_reply_code::INTEGER AS conversation_connect_reply_code,
                                supported_extensions::VARCHAR[] AS conversation_supported_extensions,
                                ip_version::INTEGER AS conversation_ip_version,
                                start_tls_ok::BOOLEAN AS conversation_start_tls_ok,
                                start_tls_reply_code::INTEGER AS conversation_start_tls_reply_code,
                                error_message::VARCHAR AS conversation_error_message,
                                error::VARCHAR AS conversation_error,
                                connection_time_ms::BIGINT AS conversation_connection_time_ms,
                                software::VARCHAR AS conversation_software,
                                software_version::VARCHAR AS conversation_software_version,
                                timestamp::DOUBLE AS conversation_timestamp,
                                month,
                                year
                          FROM extract_unnested_hosts_from_smtp
                          )
                      SELECT * FROM extract_hosts_and_conversation_from_smtp
                      )
                      TO '${smtpHostDestination}' (FORMAT parquet, PARTITION_BY (year, month), OVERWRITE_OR_IGNORE, FILENAME_PATTERN 'hosts_{uuid}' )
                """, Map.of(
                        "allResultsQuery", allResultsQuery,
                        "smtpHostDestination", this.smtpHostDestination
        ));
        logger.debug("copySmtpHosts = {}", copySmtpHosts);
        getJdbcClient().sql(copySmtpHosts).update();



    }


}