package be.dnsbelgium.mercator.batch;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.persistence.DuckDataSource;
import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.test.TestUtils;
import be.dnsbelgium.mercator.tls.domain.FullScanEntity;
import be.dnsbelgium.mercator.tls.domain.SingleVersionScan;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import be.dnsbelgium.mercator.tls.domain.TlsProtocolVersion;
import be.dnsbelgium.mercator.tls.domain.certificates.Certificate;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.cert.CertificateException;

import static be.dnsbelgium.mercator.tls.domain.certificates.CertificateReader.readTestCertificate;

public class JsonTest {

  private static final Logger logger = LoggerFactory.getLogger(JsonTest.class);




  @Test
  public void tls() throws IOException, CertificateException {
    // TODO: check if we still need this test
    Certificate certificate = Certificate.from(readTestCertificate("blackanddecker.be.pem"));

    SingleVersionScan singleVersionScan = SingleVersionScan.of(TlsProtocolVersion.TLS_1_0, new InetSocketAddress("abc.be", 443));
    singleVersionScan.setConnectOK(false);
    singleVersionScan.setErrorMessage("go away");
    singleVersionScan.setPeerCertificate(certificate);

    ObjectMother objectMother = new ObjectMother();

    FullScanEntity fullScanEntity = objectMother.fullScanEntity("example.org");
    logger.info("fullScanEntity = {}", fullScanEntity);

    VisitRequest visitRequest = new VisitRequest("aakjkjkj-ojj", "tls.org");

    TlsCrawlResult tlsCrawlResult = TlsCrawlResult.fromCache("www.tls.org", visitRequest, fullScanEntity, singleVersionScan);

    ObjectWriter writer = TestUtils.jsonWriter();

    String json = writer.writeValueAsString(tlsCrawlResult);
    logger.info("json = \n{}", json);

    writer.writeValue(new File("tls_test.json"), tlsCrawlResult);

    JdbcClient client = JdbcClient.create(DuckDataSource.memory());
    client
            .sql("copy (from 'tls_test.json') to 'tls_output.parquet' ")
            .update();

    /*
    Columns missing
      visit_id
      domain_name
      full_scan
      host_name_matches_certificate
      host_name
      leaf_certificate
      certificate_expired
      certificate_too_soon
      chain_trusted_by_java_platform
      full_scan_crawl_timestamp
      accepted_ciphers_ssl_2_0

     */

  }

}
