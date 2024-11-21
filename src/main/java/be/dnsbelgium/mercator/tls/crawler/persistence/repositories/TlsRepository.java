package be.dnsbelgium.mercator.tls.crawler.persistence.repositories;

import be.dnsbelgium.mercator.tls.crawler.persistence.entities.CertificateEntity;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.CrawlResultEntity;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.FullScanEntity;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import be.dnsbelgium.mercator.tls.domain.certificates.Certificate;
import com.github.f4b6a3.ulid.Ulid;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Instant;
import java.util.*;

import static be.dnsbelgium.mercator.persistence.Repository.instant;
import static be.dnsbelgium.mercator.persistence.Repository.timestamp;
import static be.dnsbelgium.mercator.persistence.VisitRepository.getList;

@Component
public class TlsRepository {

    private static final Logger logger = LoggerFactory.getLogger(TlsRepository.class);
    private final MeterRegistry meterRegistry;
    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final JdbcClient jdbcClient;

    public TlsRepository(DataSource dataSource, MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.jdbcClient = JdbcClient.create(dataSource);
        this.dataSource = dataSource;
    }

    public void persist(TlsCrawlResult tlsCrawlResult) {
        logger.debug("Persisting crawlResult");
        CrawlResultEntity crawlResultEntity = tlsCrawlResult.convertToEntity();
        if (tlsCrawlResult.isFresh()) {
            save(tlsCrawlResult.getFullScanEntity());
        }
        if (tlsCrawlResult.getCertificateChain().isPresent()) {
            saveCertificates(tlsCrawlResult.getCertificateChain().get());
        }
        save(crawlResultEntity);
    }

    private void saveCertificates(List<Certificate> chain) {
        // We have to iterate over the chain in reversed order (in case we would have foreign keys)
        for (Certificate certificate : chain.reversed()) {
            // We always call save, the insert will be a no-op when a cert with this fingerprint already exists
            // because of the 'ON CONFLICT DO NOTHING' clause
            CertificateEntity certificateEntity = certificate.asEntity();
            save(certificateEntity);
            logger.debug("certificate saved: {}", certificate);
        }
    }

    public void save(CertificateEntity certificate) {
        var sample = Timer.start(meterRegistry);
        // previously we had a primary key on sha256_fingerprint, and 'ON CONFLICT DO NOTHING' in the insert statement,
        // but we still got 'Failed to commit: PRIMARY KEY or UNIQUE constraint violated: duplicate key'
        // We removed the primary key and got rid of the error, but now we risk saving the certificate many times ...
        var signedBy = certificate.getSignedBySha256();
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("sha256_fingerprint", certificate.getSha256fingerprint())
                .addValue("version", certificate.getVersion())
                .addValue("public_key_schema", certificate.getPublicKeySchema())
                .addValue("public_key_length", certificate.getPublicKeyLength())
                .addValue("issuer", certificate.getIssuer())
                .addValue("subject", certificate.getSubject())
                .addValue("signature_hash_algorithm", certificate.getSignatureHashAlgorithm())
                .addValue("signed_by_sha256", signedBy)
                .addValue("serial_number_hex", certificate.getSerialNumberHex())
                .addValue("subject_alt_names", array(certificate.getSubjectAltNames()))
                .addValue("not_before", timestamp(certificate.getNotBefore()))
                .addValue("not_after", timestamp(certificate.getNotAfter()))
                .addValue("insert_timestamp", timestamp(Instant.now()));
        SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource)
                .withTableName("tls_certificate")
                .withoutTableColumnMetaDataAccess()
                .usingColumns(parameters.getParameterNames());
        insert.execute(parameters);
        sample.stop(meterRegistry.timer("repository.insert.tls.certificate"));
    }

    public void save(FullScanEntity fullScan) {
        var sample = Timer.start(meterRegistry);
        String id = Ulid.fast().toString();
        fullScan.setId(id);
        // TODO: set "accepted_ciphers_ssl_2_0"
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("id",                        fullScan.getId())
                .addValue("crawl_timestamp",           timestamp(fullScan.getCrawlTimestamp()))
                .addValue("ip",                        fullScan.getIp())
                .addValue("server_name",               fullScan.getServerName())
                .addValue("connect_ok",                fullScan.isConnectOk())
                .addValue("support_tls_1_3",           fullScan.isSupportTls_1_3())
                .addValue("support_tls_1_2",           fullScan.isSupportTls_1_2())
                .addValue("support_tls_1_1",           fullScan.isSupportTls_1_1())
                .addValue("support_tls_1_0",           fullScan.isSupportTls_1_0())
                .addValue("support_ssl_3_0",           fullScan.isSupportSsl_3_0())
                .addValue("support_ssl_2_0",           fullScan.isSupportSsl_2_0())
                .addValue("selected_cipher_tls_1_3",   fullScan.getSelectedCipherTls_1_3() )
                .addValue("selected_cipher_tls_1_2",   fullScan.getSelectedCipherTls_1_2())
                .addValue("selected_cipher_tls_1_1",   fullScan.getSelectedCipherTls_1_1())
                .addValue("selected_cipher_tls_1_0",   fullScan.getSelectedCipherTls_1_0())
                .addValue("selected_cipher_ssl_3_0",   fullScan.getSelectedCipherSsl_3_0())
                .addValue("lowest_version_supported",  fullScan.getLowestVersionSupported())
                .addValue("highest_version_supported", fullScan.getHighestVersionSupported())
                .addValue("error_tls_1_3",             fullScan.getErrorTls_1_3())
                .addValue("error_tls_1_2",             fullScan.getErrorTls_1_2())
                .addValue("error_tls_1_1",             fullScan.getErrorTls_1_1())
                .addValue("error_tls_1_0",             fullScan.getErrorTls_1_0())
                .addValue("error_ssl_3_0",             fullScan.getErrorSsl_3_0())
                .addValue("error_ssl_2_0",             fullScan.getErrorSsl_2_0())
                .addValue("millis_tls_1_3",            fullScan.getMillis_tls_1_3())
                .addValue("millis_tls_1_2",            fullScan.getMillis_tls_1_2())
                .addValue("millis_tls_1_1",            fullScan.getMillis_tls_1_1())
                .addValue("millis_tls_1_0",            fullScan.getMillis_tls_1_0())
                .addValue("millis_ssl_3_0",            fullScan.getMillis_ssl_3_0())
                .addValue("millis_ssl_2_0",            fullScan.getMillis_ssl_2_0())
                .addValue("total_duration_in_ms",      fullScan.getTotalDurationInMs());
        parameters.getParameterNames();
        SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource)
                .withTableName("tls_full_scan")
                .withoutTableColumnMetaDataAccess()
                .usingColumns(parameters.getParameterNames());
        insert.execute(parameters);
        sample.stop(meterRegistry.timer("repository.insert.tls.full.scan"));
    }

    public void save(CrawlResultEntity crawlResult) {
        var sample = Timer.start(meterRegistry);
        String leafCertFingerPrint = (crawlResult.getLeafCertificateEntity() == null) ? null : crawlResult.getLeafCertificateEntity().getSha256fingerprint();
        SqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("visit_id",                       crawlResult.getVisitId())
                .addValue("domain_name",                    crawlResult.getDomainName())
                .addValue("crawl_timestamp",                timestamp(crawlResult.getCrawlTimestamp()))
                .addValue("full_scan",                      crawlResult.getFullScanEntity().getId())
                .addValue("host_name_matches_certificate",  crawlResult.isHostNameMatchesCertificate())
                .addValue("host_name",                      crawlResult.getHostName())
                .addValue("leaf_certificate",               leafCertFingerPrint)
                .addValue("certificate_expired",            crawlResult.isCertificateExpired())
                .addValue("certificate_too_soon",           crawlResult.isCertificateTooSoon())
                .addValue("chain_trusted_by_java_platform", crawlResult.isChainTrustedByJavaPlatform());
        SimpleJdbcInsert insert = new SimpleJdbcInsert(dataSource)
                .withTableName("tls_crawl_result")
                .withoutTableColumnMetaDataAccess()
                .usingColumns(parameters.getParameterNames());
        insert.execute(parameters);
        sample.stop(meterRegistry.timer("repository.insert.tls.crawl.result"));
    }

    // TODO: move to utility class
    private Array array(List<String> list) {
        return jdbcTemplate.execute(
                (ConnectionCallback<Array>) con ->
                        con.createArrayOf("text", list.toArray())
        );
    }

    private void execute(String sql) {
        logger.info("Start executing sql = {}", sql);
        jdbcClient.sql(sql).update();
        logger.info("Done executing sql {}", sql);
    }

    public List<CrawlResultEntity> find(String visitId) {
        String query = """
            select *
            from tls_crawl_result
            join tls_full_scan on tls_crawl_result.full_scan = tls_full_scan.id
            join tls_certificate on tls_crawl_result.leaf_certificate = tls_certificate.sha256_fingerprint
            where visit_id = :visit_id
        """;
        return jdbcClient
                .sql(query)
                .param("visit_id", visitId)
                .query(new CrawlResultMapper())
                .list();
    }

    private static class CrawlResultMapper implements RowMapper<CrawlResultEntity> {

        private static final FullScanMapper fullScanMapper = new FullScanMapper();
        private static final CertificateMapper certificateMapper = new CertificateMapper();

        @Override
        public CrawlResultEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            FullScanEntity fullScan = fullScanMapper .mapRow(rs, rowNum);
            CertificateEntity certificate = certificateMapper.mapRow(rs, rowNum);
            return CrawlResultEntity.builder()
                    .visitId(rs.getString("visit_id"))
                    .domainName(rs.getString("domain_name"))
                    .crawlTimestamp(instant(rs.getTimestamp("crawl_timestamp")))
                    .fullScanEntity(fullScan)
                    .leafCertificateEntity(certificate)
                    .hostName(rs.getString("host_name"))
                    .hostNameMatchesCertificate(rs.getBoolean("host_name_matches_certificate"))
                    .certificateExpired(rs.getBoolean("certificate_expired"))
                    .certificateTooSoon(rs.getBoolean("certificate_too_soon"))
                    .chainTrustedByJavaPlatform(rs.getBoolean("chain_trusted_by_java_platform"))
                    .build();
        }
    }

    private static class FullScanMapper implements RowMapper<FullScanEntity> {

        @Override
        public FullScanEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            return FullScanEntity.builder()
                    .id(rs.getString("id"))
                    .crawlTimestamp(instant(rs.getTimestamp("crawl_timestamp")))
                    .ip(rs.getString("ip"))
                    .serverName(rs.getString("server_name"))
                    .connectOk(rs.getBoolean("connect_ok"))
                    .supportTls_1_3(rs.getBoolean("support_tls_1_3"))
                    .supportTls_1_2(rs.getBoolean("support_tls_1_2"))
                    .supportTls_1_1(rs.getBoolean("support_tls_1_1"))
                    .supportTls_1_0(rs.getBoolean("support_tls_1_0"))
                    .supportSsl_3_0(rs.getBoolean("support_ssl_3_0"))
                    .supportSsl_2_0(rs.getBoolean("support_ssl_2_0"))
                    .selectedCipherTls_1_3(rs.getString("selected_cipher_tls_1_3"))
                    .selectedCipherTls_1_2(rs.getString("selected_cipher_tls_1_2"))
                    .selectedCipherTls_1_1(rs.getString("selected_cipher_tls_1_1"))
                    .selectedCipherTls_1_0(rs.getString("selected_cipher_tls_1_0"))
                    .selectedCipherSsl_3_0(rs.getString("selected_cipher_ssl_3_0"))
                    .lowestVersionSupported(rs.getString("lowest_version_supported"))
                    .highestVersionSupported(rs.getString("highest_version_supported"))
                    .errorTls_1_3(rs.getString("error_tls_1_3"))
                    .errorTls_1_2(rs.getString("error_tls_1_2"))
                    .errorTls_1_1(rs.getString("error_tls_1_1"))
                    .errorTls_1_0(rs.getString("error_tls_1_0"))
                    .errorSsl_3_0(rs.getString("error_ssl_3_0"))
                    .errorSsl_2_0(rs.getString("error_ssl_2_0"))
                    .millis_tls_1_3(rs.getLong("millis_tls_1_3"))
                    .millis_tls_1_2(rs.getLong("millis_tls_1_2"))
                    .millis_tls_1_1(rs.getLong("millis_tls_1_1"))
                    .millis_tls_1_0(rs.getLong("millis_tls_1_0"))
                    .millis_ssl_3_0(rs.getLong("millis_ssl_3_0"))
                    .millis_ssl_2_0(rs.getLong("millis_ssl_2_0"))
                    .build();
        }
    }

    private static class CertificateMapper implements RowMapper<CertificateEntity> {

        @Override
        public CertificateEntity mapRow(ResultSet rs, int rowNum) throws SQLException {
            return CertificateEntity.builder()
                    .sha256fingerprint(rs.getString("sha256_fingerprint"))
                    .version(rs.getInt("version"))
                    .publicKeySchema(rs.getString("public_key_schema"))
                    .publicKeyLength(rs.getInt("public_key_length"))
                    .notBefore(instant(rs.getTimestamp("not_before")))
                    .notAfter(instant(rs.getTimestamp("not_after")))
                    .issuer(rs.getString("issuer"))
                    .subject(rs.getString("subject"))
                    .signatureHashAlgorithm(rs.getString("signature_hash_algorithm"))
                    .signedBySha256(rs.getString("signed_by_sha256"))
                    .subjectAltNames(getList(rs, "subject_alt_names"))
                    .serialNumberHex(rs.getString("serial_number_hex"))
                    .build();
        }
    }



    public void createTablesTls() {
        var ddl_certificate = """
                create table if not exists tls_certificate
                (
                    sha256_fingerprint       varchar(256), -- logically a primary key
                    version                  integer      not null,
                    public_key_schema        varchar(256),
                    public_key_length        integer,
                    not_before               timestamp,
                    not_after                timestamp,
                    issuer                   varchar(500),
                    subject                  varchar(500),
                    signature_hash_algorithm varchar(256),
                    signed_by_sha256         varchar(256),  --logically references tls_certificate,
                    subject_alt_names        varchar[],
                    serial_number_hex        varchar(64),
                    insert_timestamp         timestamp default CURRENT_TIMESTAMP
                )
                """;
        execute(ddl_certificate);
        var ddl_full_scan = """
                create table if not exists tls_full_scan
                (
                    id                        varchar                     primary key,
                    crawl_timestamp           timestamp                   not null,
                    ip                        varchar(255),
                    server_name               varchar(128)                not null,
                    connect_ok                boolean                     not null,
                    support_tls_1_3           boolean,
                    support_tls_1_2           boolean,
                    support_tls_1_1           boolean,
                    support_tls_1_0           boolean,
                    support_ssl_3_0           boolean,
                    support_ssl_2_0           boolean,
                    selected_cipher_tls_1_3   varchar,
                    selected_cipher_tls_1_2   varchar,
                    selected_cipher_tls_1_1   varchar,
                    selected_cipher_tls_1_0   varchar,
                    selected_cipher_ssl_3_0   varchar,
                    accepted_ciphers_ssl_2_0  varchar[],
                    lowest_version_supported  varchar,
                    highest_version_supported varchar,
                    error_tls_1_3             varchar,
                    error_tls_1_2             varchar,
                    error_tls_1_1             varchar,
                    error_tls_1_0             varchar,
                    error_ssl_3_0             varchar,
                    error_ssl_2_0             varchar,
                    millis_ssl_2_0            integer,
                    millis_ssl_3_0            integer,
                    millis_tls_1_0            integer,
                    millis_tls_1_1            integer,
                    millis_tls_1_2            integer,
                    millis_tls_1_3            integer,
                    total_duration_in_ms      integer
                )
                """;
        execute(ddl_full_scan);
        var ddl_tls_crawl_result = """
                create table if not exists tls_crawl_result
                (
                    visit_id                       varchar(26)                     not null,
                    domain_name                    varchar(128)                    not null,
                    crawl_timestamp                timestamp                       not null,
                    full_scan                      varchar                         not null,  -- logically references tls_full_scan,
                    host_name_matches_certificate  boolean,
                    host_name                      varchar(128)                    not null,
                    leaf_certificate               varchar(256),
                    certificate_expired            boolean,
                    certificate_too_soon           boolean,
                    chain_trusted_by_java_platform boolean
                )
                """;
        execute(ddl_tls_crawl_result);
    }

}
