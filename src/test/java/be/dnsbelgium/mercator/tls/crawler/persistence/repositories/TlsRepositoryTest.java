package be.dnsbelgium.mercator.tls.crawler.persistence.repositories;

import be.dnsbelgium.mercator.DuckDataSource;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.CertificateEntity;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.CrawlResultEntity;
import be.dnsbelgium.mercator.tls.crawler.persistence.entities.FullScanEntity;
import be.dnsbelgium.mercator.tls.domain.certificates.Certificate;
import com.github.f4b6a3.ulid.Ulid;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.util.List;

import static be.dnsbelgium.mercator.tls.domain.certificates.CertificateReader.readTestCertificate;


class TlsRepositoryTest {

    static DuckDataSource dataSource;
    static JdbcClient jdbcClient;
    static TlsRepository tlsRepository;
    static MeterRegistry meterRegistry = new SimpleMeterRegistry();

    private static final Logger logger = LoggerFactory.getLogger(TlsRepositoryTest.class);

    @BeforeAll
    public static void init() {
        dataSource = new DuckDataSource("jdbc:duckdb:");
        tlsRepository = new TlsRepository(dataSource, meterRegistry);
        tlsRepository.createTablesTls();
        jdbcClient = JdbcClient.create(dataSource);
    }

    @Test
    void tls_crawl_result() {
        CertificateEntity certificateEntity = CertificateEntity.builder()
                .sha256fingerprint("12345")
                .serialNumberHex("CA FE BE")
                .subjectAltNames(List.of("abc.be", "www.abc.be"))
                .build();

        tlsRepository.save(certificateEntity);
        logger.info("certificateEntity = {}", certificateEntity);

        FullScanEntity fullScanEntity = FullScanEntity.builder()
                .serverName("dnsbelgium.be")
                .connectOk(true)
                .highestVersionSupported("TLS 1.3")
                .lowestVersionSupported("TLS 1.2")
                .supportTls_1_3(true)
                .supportTls_1_2(true)
                .supportTls_1_1(false)
                .supportTls_1_0(false)
                .supportSsl_3_0(false)
                .supportSsl_2_0(false)
                .errorTls_1_1("No can do")
                .errorTls_1_0("Go away")
                .errorSsl_3_0("Why?")
                .errorSsl_2_0("Protocol error")
                .ip("10.20.30.40")
                .crawlTimestamp(Instant.now())
                .build();

        CrawlResultEntity crawlResultEntity = CrawlResultEntity.builder()
                .fullScanEntity(fullScanEntity)
                .domainName("dns.be")
                .hostName("www.dns.be")
                .visitId(Ulid.fast().toString())
                .crawlTimestamp(Instant.now())
                .leafCertificateEntity(certificateEntity)
                .build();

        tlsRepository.save(fullScanEntity);
        tlsRepository.save(crawlResultEntity);
        logger.info("AFTER: crawlResultEntity = {}", crawlResultEntity);

        List<CrawlResultEntity> found = tlsRepository.find(crawlResultEntity.getVisitId());
        System.out.println("found = " + found);
        for (CrawlResultEntity resultEntity : found) {
            System.out.println("resultEntity = " + resultEntity);
        }

    }


    @Test
    public void certificate() throws CertificateException, IOException {
        X509Certificate x509Certificate = readTestCertificate("dnsbelgium.be.pem");
        Certificate certificate = Certificate.from(x509Certificate);
        logger.info("info = {}", certificate);
        logger.info("info = {}", certificate.prettyString());
        tlsRepository.save(certificate.asEntity());
        tlsRepository.save(certificate.asEntity());
    }

}