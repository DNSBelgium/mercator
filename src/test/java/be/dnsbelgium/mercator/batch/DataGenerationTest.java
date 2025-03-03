package be.dnsbelgium.mercator.batch;

import be.dnsbelgium.mercator.common.VisitIdGenerator;
import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.tls.domain.FullScan;
import be.dnsbelgium.mercator.tls.domain.FullScanEntity;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import be.dnsbelgium.mercator.tls.domain.TlsScanner;
import be.dnsbelgium.mercator.tls.ports.TlsCrawler;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.net.InetSocketAddress;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class DataGenerationTest {

  @Autowired TlsCrawler tlsCrawler;
  @Autowired TlsScanner tlsScanner;

  ObjectMapper objectMapper = new ObjectMapper();

  private static final Logger logger = LoggerFactory.getLogger(DataGenerationTest.class);

  @BeforeEach
  public void init() {
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.registerModule(new Jdk8Module());
  }

  @Test
  public void fetchData() throws IOException {
    TlsCrawlResult result1 = tlsVisit("dnsbelgium.be");
    TlsCrawlResult result2 = tlsVisit("google.be");
    List<TlsCrawlResult> results = Arrays.asList(result1, result2);
    StringWriter writer = new StringWriter();
    objectMapper.writeValue(writer, results);
    String json = writer.toString();
    logger.info("json = {}", json);
    objectMapper.writeValue(new File("tls-testdata.json"), results);
  }

  @Test
  public void tls_v2() throws IOException {
    // better to serialize a FullScanEntity instead of a TlsCrawlResult ?
    // todo: decide how to persist certificates ?
//    TlsCrawlResult result1 = tlsVisit("dnsbelgium.be");
//    TlsCrawlResult result2 = tlsVisit("google.be");
//    List<TlsCrawlResult> results = Arrays.asList(result1, result2);
    StringWriter writer = new StringWriter();

    String hostname = "www.dnsbelgium.be";
    InetSocketAddress address = new InetSocketAddress(hostname, 443);
    FullScan fullScan = tlsScanner.scan(address);
    FullScanEntity fullScanEntity = TlsCrawlResult.convert(Instant.now(), fullScan);
    logger.info("fullScanEntity = {}", fullScanEntity);

    objectMapper.writeValue(writer, fullScanEntity);
    String json = writer.toString();
    logger.info("json = {}", json);
    objectMapper.writeValue(new File("tls-fullScanEntity.json"), fullScanEntity);

  }

  private TlsCrawlResult tlsVisit(String domainName) {
    VisitRequest visitRequest = new VisitRequest( VisitIdGenerator.generate(), domainName);
    return tlsCrawler.visit("www." + domainName, visitRequest);
  }

  /*

  TlsCrawlResult.visitRequest is included in output
  TlsCrawlResult.fullScanEntity is included in output

  TlsCrawlResult.hostName is NOT included in output
  TlsCrawlResult.crawlTimestamp is NOT included in output



  in output:

     - visitRequest: 2 fiels
     - fullScanEntity: 31 fields
     - peerCertificate :   Optional <Certificate>
     - certificateChain:   List of Certificate
     - fresh


   */

}
