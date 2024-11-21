package eu.bosteels.mercator.mono.persistence;

import be.dnsbelgium.mercator.DuckDataSource;
import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.dns.domain.DnsCrawlResult;

import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.persistence.Request;
import be.dnsbelgium.mercator.dns.persistence.Response;
import be.dnsbelgium.mercator.dns.persistence.ResponseGeoIp;
import eu.bosteels.mercator.mono.mvc.Stats;
import eu.bosteels.mercator.mono.scheduling.CrawlRate;
import eu.bosteels.mercator.mono.scheduling.Done;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Component;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;


@SuppressWarnings({"SqlDialectInspection"})
@Component
public class Repository {

    private static final Logger logger = LoggerFactory.getLogger(Repository.class);
    private final JdbcTemplate jdbcTemplate;

    public enum Frequency {PerMinute, PerHour}

    @Autowired
    public Repository(DuckDataSource dataSource) {
        this.jdbcTemplate = new JdbcTemplate(dataSource);
    }

    public Repository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long nextid() {
        return jdbcTemplate.queryForObject("select nextval('serial')", Long.class);
    }

    public void saveOperation(Instant ts, String sql, Duration duration) {
        String insert = "insert into operations(ts, sql, millis) values (?,?,?)";
        jdbcTemplate.update(insert, Timestamp.from(ts), sql, duration.toMillis());
    }

    public List<Done> findDone(String domainName) {
        return jdbcTemplate.query(
                """
                    select visit_id, domain_name, done
                    from done
                    where domain_name = ?
                    order by done desc
                """,
                (rs, rowNum) -> {
                    var visitId = rs.getString("visit_id");
                    var domain_name = rs.getString("domain_name");
                    var done = instant(rs.getTimestamp("done"));
                    return new Done(visitId, domain_name, done);
                }, domainName);
    }

    public void markDone(VisitRequest visitRequest) {
        logger.debug("Marking as done: {}", visitRequest);
        jdbcTemplate.update(
                "insert into done(visit_id, domain_name, done) values(?,?,current_timestamp)",
                visitRequest.getVisitId(), visitRequest.getDomainName()
        );
        jdbcTemplate.update("delete from work where visit_id = ?", visitRequest.getVisitId());
    }

    @SuppressWarnings("unused")
    public Optional<DnsCrawlResult> findDnsCrawlResultByVisitId(String visitId) {
        var select = """
                select
                    dns_response.id as response_id,
                    dns_request.id as request_id,
                    *
                from dns_request
                left join dns_response on dns_response.dns_request	= dns_request.id
                left join response_geo_ips on response_geo_ips.dns_response = dns_response.id
                where visit_id = ?
                order by request_id, response_id, ip
                """;
        var requests = jdbcTemplate.query(select,
                (rs, rowNum) -> {
                    String response_id = rs.getString("response_id");
                    String request_id = rs.getString("request_id");
                    List<ResponseGeoIp> geoIps = List.of();
                    Long asn = getLong(rs, "asn");
                    if (asn != null) {
                        String asn_org = rs.getString("asn_organisation");
                        String ip = rs.getString("ip");
                        String country = rs.getString("country");
                        int ip_version = rs.getInt("ip_version");
                        geoIps = List.of(new ResponseGeoIp(Pair.of(asn, asn_org), country, ip_version, ip));
                    }
                    String record_data	= rs.getString("record_data");
                    Long ttl = getLong(rs, "ttl");
                    var response = new Response(response_id, record_data, ttl, geoIps);

                    String rtype = rs.getString("record_type");
                    Timestamp ts = rs.getTimestamp("crawl_timestamp");
                    ZonedDateTime crawl_timestamp = zonedDateTime(ts, ZoneId.systemDefault());

                    return Request.builder()
                            .id(request_id)
                            .domainName(rs.getString("domain_name"))
                            .rcode(rs.getInt("rcode"))
                            .problem(rs.getString("problem"))
                            .prefix(rs.getString("prefix"))
                            .recordType(RecordType.valueOf(rtype))
                            .crawlTimestamp(crawl_timestamp)
                            .responses(List.of(response))
                            .build();
                },
                visitId);

        if (requests.isEmpty()) {
            return Optional.empty();
        }
        var request = DnsCrawlResult.of(requests);
        logger.info("findDnsCrawlResultByVisitId: {}", request);
        return Optional.of(request);
    }
    public Optional<DnsCrawlResult> findDnsCrawlResultByVisitId_v2(String visitId) {
        var select = """
                select
                    dns_response.id as response_id,
                    dns_request.id as request_id,
                    *
                from dns_request
                left join dns_response on dns_response.dns_request	= dns_request.id
                left join response_geo_ips on response_geo_ips.dns_response = dns_response.id
                where visit_id = ?
                order by request_id, response_id, ip
                """;

        var rowCallbackHandler = new DnsResultsRowCallbackHandler();
        jdbcTemplate.query(select, rowCallbackHandler, visitId);
        return rowCallbackHandler.getDnsCrawlResult();
    }

    public static Long getLong(ResultSet rs, String columnName) throws SQLException {
        long value = rs.getLong(columnName);
        if (rs.wasNull()) {
            return null;
        }
        return value;
    }

    @SuppressWarnings("DataFlowIssue")
    public Stats getStats() {
        long todo = jdbcTemplate.queryForObject("select count(1) from work", Long.class);
        long done = jdbcTemplate.queryForObject("select count(1) from done", Long.class);
        return new Stats(todo, done);
    }

    private record CrawlRateMapper(Frequency frequency) implements RowMapper<CrawlRate> {

        @Override
            public CrawlRate mapRow(ResultSet rs, int rowNum) throws SQLException {
                Timestamp ts = rs.getTimestamp("ts");
                var when = zonedDateTime(ts, ZoneId.systemDefault());
                int count = rs.getInt("count");
                if (Objects.requireNonNull(frequency) == Frequency.PerHour) {
                    return new CrawlRate(when, count, count / 60.0, count / 3600.0);
                }
                return new CrawlRate(when, count * 60.0, count, count / 60.0);
            }
        }

    public List<CrawlRate> getRecentCrawlRates(Frequency frequency, int limit) {
        CrawlRateMapper mapper = new CrawlRateMapper(frequency);
        var select = String.format("""
            select
                date_trunc('%s', done) as ts,
                count(1) as count
            from done
            group by ts
            order by ts desc
            limit %s
            """, toSqlPrecision(frequency), limit);
        return jdbcTemplate.query(select, mapper);
    }

    private String toSqlPrecision(Frequency frequency) {
        return switch (frequency) {
            case PerHour -> "hour";
            case PerMinute -> "minute";
        };
    }

    public static Timestamp timestamp(ZonedDateTime zonedDateTime) {
        if (zonedDateTime == null) {
            return null;
        }
        return Timestamp.from(zonedDateTime.toInstant());
    }

    static ZonedDateTime zonedDateTime(Timestamp timestamp, ZoneId zoneId) {
        if (timestamp == null) {
            return null;
        }
        return ZonedDateTime.of(timestamp.toLocalDateTime(), zoneId);
    }

    public static Timestamp timestamp(Instant instant) {
        if (instant == null) {
          return null;
        }
        return Timestamp.from(instant);
    }

    public static Instant instant(Timestamp timestamp) {
        return (timestamp == null) ? null : timestamp.toInstant();
    }

    public static String values(int paramCount) {
        return " values( " + StringUtils.repeat("?", ", ", paramCount) + ")";
    }

}
