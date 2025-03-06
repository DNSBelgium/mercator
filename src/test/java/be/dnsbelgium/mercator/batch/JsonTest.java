package be.dnsbelgium.mercator.batch;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeatures;
import be.dnsbelgium.mercator.persistence.DuckDataSource;
import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.tls.domain.FullScanEntity;
import be.dnsbelgium.mercator.tls.domain.SingleVersionScan;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import be.dnsbelgium.mercator.tls.domain.TlsProtocolVersion;
import be.dnsbelgium.mercator.tls.domain.certificates.Certificate;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.f4b6a3.ulid.Ulid;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.json.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.PathResource;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.File;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static be.dnsbelgium.mercator.tls.domain.certificates.CertificateReader.readTestCertificate;

public class JsonTest {

  private static final Logger logger = LoggerFactory.getLogger(JsonTest.class);

  @Test
  @Disabled // TODO: first generate the data or remove the test
  public void readJson() throws Exception {
    String inputFileName = "./target/test-outputs/web.json";
    JsonItemReader<WebCrawlResult> jsonItemReader = new JsonItemReader<>();
    jsonItemReader.setResource(new PathResource(inputFileName));
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    JsonObjectReader<WebCrawlResult> jsonObjectReader = new JacksonJsonObjectReader<>(objectMapper, WebCrawlResult.class);
    jsonItemReader.setJsonObjectReader(jsonObjectReader);
    jsonItemReader.open(new ExecutionContext());
    WebCrawlResult webCrawlResult = jsonItemReader.read();
    System.out.println("webCrawlResult = " + webCrawlResult);
  }

  @Test
  @Disabled // TODO: first generate the data
  public void read() throws Exception {
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(javaTimeModule);
    JacksonJsonObjectReader<WebCrawlResult> jsonObjectReader = new JacksonJsonObjectReader<>(objectMapper, WebCrawlResult.class);
    String inputFileName = "./target/test-outputs/web.json";
    jsonObjectReader.open(new PathResource(inputFileName));
    WebCrawlResult webCrawlResult = jsonObjectReader.read();
    while (webCrawlResult != null) {
      System.out.println("webCrawlResult = " + webCrawlResult);
      webCrawlResult = jsonObjectReader.read();
    }
  }

  @Test
  @Disabled
  public void parquetToJson() throws Exception {
    JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());
    // create a parquet file
    jdbcClient.sql("copy (FROM './target/test-outputs/web.json') to 'web.parquet' ");
    // get one row from the parquet file
    @SuppressWarnings("SqlResolve")
    Optional<String> json = jdbcClient
            .sql("select list(to_json(p)) from 'web.parquet' p where visitId = ? limit 1")
            .param("v101")
            .query(String.class)
            .optional();
    if (json.isPresent()) {
      //System.out.println("json = " + json.get());
      JavaTimeModule javaTimeModule = new JavaTimeModule();
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(javaTimeModule);
      JacksonJsonObjectReader<WebCrawlResult> jsonObjectReader = new JacksonJsonObjectReader<>(objectMapper, WebCrawlResult.class);
      jsonObjectReader.open(new ByteArrayResource(json.get().getBytes(StandardCharsets.UTF_8)));
      WebCrawlResult webCrawlResult = jsonObjectReader.read();
      System.out.println("webCrawlResult = " + webCrawlResult);
      if (webCrawlResult != null) {
        webCrawlResult.getHtmlFeatures().forEach(System.out::println);
      }

    }
  }

  @Disabled
  @Test
  public void readObject() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());
    // create a parquet file
    jdbcClient.sql("copy (FROM './target/test-outputs/web.json') to 'web.parquet' ");
    // get one row from the parquet file
    Optional<String> json = jdbcClient
            .sql("select to_json(p) from 'web.parquet' p where visitId = ? limit 1")
            .param("v101")
            .query(String.class)
            .optional();
    if (json.isPresent()) {
      WebCrawlResult webCrawlResult = objectMapper.readValue(json.get(), WebCrawlResult.class);
      logger.info("webCrawlResult = {}", webCrawlResult);
      for (HtmlFeatures htmlFeature : webCrawlResult.getHtmlFeatures()) {
        logger.info("htmlFeature = {}", htmlFeature);
      }
    }
  }

//  @Test
//  public void tls_to_json() throws Exception {
//    TlsScanner scanner = new TlsScanner()
//    TlsCrawler crawler = new TlsCrawler();
//  }

  @Test
  public void ulid() {
    long x1 = Ulid.fast().getMostSignificantBits();
    long x2 = Ulid.fast().getMostSignificantBits();
    long x3 = Instant.now().toEpochMilli();
    System.out.println(x1);
    System.out.println(x2);
    System.out.println(x3);
  }

  @Test
  public void tls() throws IOException, CertificateException {
    // we gaan een FullScanEntity opvullen en wegschrijven naar JSON en parquet
    // en kijken of formaat dan overeenkomt met wat Mercator v1 nu exporteert ...

    Instant fullScanInstant = LocalDateTime.of(2025,1,25, 23, 59).toInstant(ZoneOffset.UTC);

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

    JavaTimeModule javaTimeModule = new JavaTimeModule();
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.setPropertyNamingStrategy(new PropertyNamingStrategies.SnakeCaseStrategy());

    objectMapper.registerModule(javaTimeModule);
    ObjectWriter writer = objectMapper.writerWithDefaultPrettyPrinter();


    //.with(PropertyNamingStrategies.SnakeCaseStrategy)

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
