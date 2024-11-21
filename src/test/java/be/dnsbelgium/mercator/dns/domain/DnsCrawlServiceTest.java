package be.dnsbelgium.mercator.dns.domain;

import be.dnsbelgium.mercator.common.VisitIdGenerator;
import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.dns.DnsCrawlerConfigurationProperties;
import be.dnsbelgium.mercator.dns.domain.resolver.DnsResolver;
import be.dnsbelgium.mercator.dns.dto.DnsRequest;
import be.dnsbelgium.mercator.dns.dto.RRecord;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.persistence.Request;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.xbill.DNS.Name;
import org.xbill.DNS.Rcode;
import org.xbill.DNS.TextParseException;

import java.time.ZonedDateTime;
import java.util.*;

import static be.dnsbelgium.mercator.dns.dto.DnsRequest.nxdomain;
import static be.dnsbelgium.mercator.dns.dto.DnsRequest.success;
import static be.dnsbelgium.mercator.dns.dto.RecordType.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@SpringJUnitConfig({
        DnsCrawlService.class,
        MetricsAutoConfiguration.class,
        CompositeMeterRegistryAutoConfiguration.class
})
class DnsCrawlServiceTest {

  //@MockitoBean
  @MockBean
  DnsResolver dnsResolver;

  //@MockitoBean
  @MockBean
  Enricher enricher;

  //  @MockitoBean
  @MockBean
  DnsCrawlerConfigurationProperties dnsCrawlerConfig;

  @Autowired
  DnsCrawlService dnsCrawlService;

  Random random = new Random();

  private static final Logger logger = LoggerFactory.getLogger(DnsCrawlServiceTest.class);

  // The actual String values have no impact on the tests, it just looks a bit better to use realistic data
  public static final String IP1 = "10.20.30.40";
  public static final String IP2 = "20.20.30.40";
  public static final String IP3 = "30.20.30.40";
  public static final String IPv6 = "2a02:e980:53:0:0:0:0:8b";
  public static final Long TTL = 3600L;

  public static final String SOA_RDATA = "ns1.dns.be. be-hostmaster.dnsbelgium.be. 2020144915 10800 1800 3600000 3600";

  @Test
  public void invalidDomainNameIsIgnored() {
    VisitRequest visitRequest = make("--invalid--.be");
    var x = dnsCrawlService.retrieveDnsRecords(visitRequest);
    System.out.println("x = " + x);
    System.out.println("x.getStatus = " + x.getStatus());
    verify(enricher, never()).enrichResponses(any());
    //verify(requestRepository, never()).saveAll(any());
    verify(dnsResolver, never()).lookup(any(String.class), any(Name.class), any(RecordType.class));
  }

  private VisitRequest make(String domainName) {
    return new VisitRequest(VisitIdGenerator.generate(), domainName);
  }


  @Test
  public void query_for_ascii_and_save_unicode() {
    VisitRequest visitRequest = make( "dnsbelgië.be");
    when(dnsCrawlerConfig.getSubdomains()).thenReturn(new HashMap<>(Map.of(
        "@",   List.of(A, SOA),
        "www", List.of(A)
    )));
    Name a_label = Name.fromConstantString("xn--dnsbelgi-01a.be");
    expectResponses("@", A,   a_label , "10.20.30.40");
    expectResponses("@", SOA, a_label, "a SOA record");
    expectResponses("www", A, a_label);
    DnsCrawlResult dnsCrawlResult = dnsCrawlService.retrieveDnsRecords(visitRequest);
    verify(enricher).enrichResponses(any());
    List<Request> requests = dnsCrawlResult.getRequests();
    logger.info("requests.size = {}", requests.size());
    for (Request request : requests) {
      logRequest(request);
      assertThat(request.getDomainName())
          .withFailMessage("We should always save U-label in the database")
          .isEqualTo("dnsbelgië.be");
    }
    verify(dnsResolver).lookup("@"  , a_label, A);
    verify(dnsResolver).lookup("@"  , a_label, SOA);
    verify(dnsResolver).lookup("www", a_label, A);
    verify(dnsResolver).lookup("@"  , a_label, A);
  }

  @Test
  void only_one_lookup_done_when_first_response_is_nxdomain() throws TextParseException {
    VisitRequest visitRequest = make( "dnsbelgium.be");
    Name dnsbelgium = Name.fromString("dnsbelgium.be");
    DnsRequest expected_A = nxdomain("@", A);
    when(dnsResolver.lookup("@", dnsbelgium, A)).thenReturn(expected_A);
    DnsCrawlResult dnsCrawlResult = dnsCrawlService.retrieveDnsRecords(visitRequest);
    logger.info("dnsCrawlResult = {}", dnsCrawlResult);
    List<Request> requests = dnsCrawlResult.getRequests();
    assertThat(requests).hasSize(1);
    assertThat(requests.get(0).isOk()).isFalse();
    assertThat(requests.get(0).getProblem()).isEqualTo("nxdomain");
    assertThat(requests.get(0).getResponses()).isEmpty();
    assertThat(requests.get(0).getNumOfResponses()).isEqualTo(0);
    // lookup() is called only once
    verify(dnsResolver).lookup(any(String.class), any(Name.class), any(RecordType.class));
  }

  @Test void buildEntitySuccess() {
    String visitId = VisitIdGenerator.generate();
    VisitRequest visitRequest = new VisitRequest(visitId, "dnsbelgium.be");
    var records = List.of(new RRecord(TTL, IP1), new RRecord(TTL, IP2));
    var dnsRequest = DnsRequest.success("@", A, records);
    ZonedDateTime before = ZonedDateTime.now();
    Request request = dnsCrawlService.buildEntity(visitRequest, dnsRequest);
    ZonedDateTime after = ZonedDateTime.now();
    logRequest(request);
    assertThat(request.getRecordType()).isEqualTo(A);
    assertThat(request.getId()).isNull();
    assertThat(request.getResponses()).hasSize(2);
    assertThat(request.getPrefix()).isEqualTo("@");
    assertThat(request.getNumOfResponses()).isEqualTo(2);
    assertThat(request.getRcode()).isEqualTo(0);
    assertThat(request.getVisitId()).isEqualTo(visitId);
    assertThat(request.getProblem()).isNull();
    assertThat(request.getCrawlTimestamp()).isBeforeOrEqualTo(ZonedDateTime.now());
    assertThat(request.getCrawlTimestamp()).isBetween(before,after);
    assertThat(request.getDomainName()).isEqualTo("dnsbelgium.be");
    assertThat(request.isOk()).isTrue();
    assertThat(request.getResponses().get(0).getRecordData()).isEqualTo(IP1);
    assertThat(request.getResponses().get(1).getRecordData()).isEqualTo(IP2);
    assertThat(request.getResponses().get(0).getTtl()).isEqualTo(TTL);
    assertThat(request.getResponses().get(1).getTtl()).isEqualTo(TTL);
    verify(enricher, never()).enrichResponses(any());
  }

  private void logRequest(Request request) {
    logger.info("request = {}", request);
  }

  @Test void buildEntityNxdomain() {
    var visitId = VisitIdGenerator.generate();
    VisitRequest visitRequest = new VisitRequest(visitId, "dnsbelgium.be");
    var nxdomain = DnsRequest.nxdomain("@", A);
    ZonedDateTime before = ZonedDateTime.now();
    Request request = dnsCrawlService.buildEntity(visitRequest, nxdomain);
    ZonedDateTime after = ZonedDateTime.now();
    logRequest(request);
    assertThat(request.getRecordType()).isEqualTo(A);
    assertThat(request.getId()).isNull();
    assertThat(request.getResponses()).hasSize(0);
    assertThat(request.getPrefix()).isEqualTo("@");
    assertThat(request.getNumOfResponses()).isEqualTo(0);
    assertThat(request.getRcode()).isEqualTo(Rcode.NXDOMAIN);
    assertThat(request.getVisitId()).isEqualTo(visitId);
    assertThat(request.getProblem()).isEqualTo("nxdomain");
    assertThat(request.getCrawlTimestamp()).isBeforeOrEqualTo(ZonedDateTime.now());
    assertThat(request.getCrawlTimestamp()).isBetween(before,after);
    assertThat(request.getDomainName()).isEqualTo("dnsbelgium.be");
    assertThat(request.isOk()).isFalse();
  }

  @Test
  void retrieve_A_and_SOA_Records_for_apex_and_A_and_AAAA_for_www() throws TextParseException {
    Name dnsbelgium = Name.fromString("dnsbelgium.be");
    when(dnsCrawlerConfig.getSubdomains()).thenReturn(new HashMap<>(Map.of(
            "@",   List.of(A, SOA),
            "www", List.of(A, AAAA)
    )));
    // set up mock responses
    // Actually, for DnsCrawlService it does not matter how the RRecord objects look like, they don't have a type anyway
    expectResponses("@",   A,    dnsbelgium, IP1, IP2);
    expectResponses("@",   SOA,  dnsbelgium, SOA_RDATA);
    expectResponses("www", A,    dnsbelgium, IP3);
    expectResponses("www", AAAA, dnsbelgium, IPv6);
    VisitRequest visitRequest = make( "dnsbelgium.be");
    DnsCrawlResult dnsCrawlResult = dnsCrawlService.retrieveDnsRecords(visitRequest);
    List<Request> requestsSaved = dnsCrawlResult.getRequests();
    for (Request request : requestsSaved) {
      logRequest(request);
    }
    assertThat(requestsSaved).hasSize(4);
    for (Request request : requestsSaved) {
      assertThat(request.getProblem()).isNull();
      assertThat(request.getId()).isNull();
      assertThat(request.isOk()).isTrue();
      assertThat(request.getRcode()).isEqualTo(0);
      assertThat(request.getVisitId()).isEqualTo(visitRequest.getVisitId());
      assertThat(request.getDomainName()).isEqualTo(visitRequest.getDomainName());
    }
    // A records for @
    assertThat(requestsSaved.get(0).getPrefix()).isEqualTo("@");
    assertThat(requestsSaved.get(0).getRecordType()).isEqualTo(A);
    assertThat(requestsSaved.get(0).getNumOfResponses()).isEqualTo(2);
    assertThat(requestsSaved.get(0).getResponses().get(0).getRecordData()).isEqualTo(IP1);
    assertThat(requestsSaved.get(0).getResponses().get(1).getRecordData()).isEqualTo(IP2);
    // SOA record for @
    assertThat(requestsSaved.get(1).getPrefix()).isEqualTo("@");
    assertThat(requestsSaved.get(1).getRecordType()).isEqualTo(SOA);
    assertThat(requestsSaved.get(1).getNumOfResponses()).isEqualTo(1);
    assertThat(requestsSaved.get(1).getResponses().get(0).getRecordData()).isEqualTo(SOA_RDATA);
    // A record for www
    assertThat(requestsSaved.get(2).getPrefix()).isEqualTo("www");
    assertThat(requestsSaved.get(2).getRecordType()).isEqualTo(A);
    assertThat(requestsSaved.get(2).getNumOfResponses()).isEqualTo(1);
    // AAAA record for www
    assertThat(requestsSaved.get(3).getPrefix()).isEqualTo("www");
    assertThat(requestsSaved.get(3).getRecordType()).isEqualTo(AAAA);
    assertThat(requestsSaved.get(3).getNumOfResponses()).isEqualTo(1);
    assertThat(requestsSaved.get(3).getResponses().get(0).getRecordData()).isEqualTo(IPv6);
    verify(enricher).enrichResponses(any());
  }

  private void expectResponses(String prefix, RecordType recordType, Name name, String... rdata) {
    List<RRecord> records = new ArrayList<>();
    for (String data : rdata) {
      records.add(new RRecord(TTL, data));
    }
    var expected = new DnsRequest(prefix, recordType, 0, null, records);
    when(dnsResolver.lookup(prefix, name, recordType)).thenReturn(expected);
  }

  @Test
  //@Disabled
  void find_all_that_we_search_for() {
    when(dnsCrawlerConfig.getSubdomains()).thenReturn(new HashMap<>(Map.of(
        "@", List.of(SOA, A, AAAA, CAA, MX, TXT, CNAME),
        "www", List.of(A, AAAA),
        "_dmarc", List.of(TXT)
    )));
    when(dnsResolver
            .lookup(any(String.class), any(Name.class), any(RecordType.class)))
            .thenReturn(randomNonEmptyResponse());

    VisitRequest visitRequest = make( "dnsbelgium.be");
    DnsCrawlResult dnsCrawlResult = dnsCrawlService.retrieveDnsRecords(visitRequest);
    List<Request> requests = dnsCrawlResult.getRequests();
    logger.info("requests.size = {}", requests.size());
    for (Request request : requests) {
      logRequest(request);
    }
    assertThat(requests).hasSize(10);
    verify(enricher, times(1)).enrichResponses(any());
  }

  private DnsRequest randomNonEmptyResponse() {
    List<RRecord> records = new ArrayList<>();
    int numResponses = random.nextInt(1,5);
    for (int i=0; i<numResponses; i++) {
      records.add(new RRecord(TTL, RandomStringUtils.randomAlphanumeric(10)));
    }
    return new DnsRequest("anything", A, 0, null, records);
  }

  @Test
  void only_find_A_at_apex() {
    when(dnsCrawlerConfig.getSubdomains()).thenReturn(new HashMap<>(Map.of(
        "@", List.of(SOA, A, AAAA, CAA, MX, TXT, CNAME),
        "www", List.of(A, AAAA),
        "_dmarc", List.of(TXT)
    )));
    var apex_a = success("@", A, List.of(new RRecord(TTL, IP1)));
    var noResponses = nxdomain("prefix", SOA);
    when(dnsResolver
            .lookup(any(String.class), any(Name.class), any(RecordType.class)))
            .thenReturn(apex_a)
            .thenReturn(noResponses);

    VisitRequest visitRequest = make( "dnsbelgium.be");
    DnsCrawlResult dnsCrawlResult = dnsCrawlService.retrieveDnsRecords(visitRequest);
    List<Request> requests = dnsCrawlResult.getRequests();
    logger.info("requests.size = {}", requests.size());
    for (Request request : requests) {
      logRequest(request);
      assertThat(request.getId()).isNull();
      assertThat(request.getVisitId()).isEqualTo(visitRequest.getVisitId());
      assertThat(request.getDomainName()).isEqualTo(visitRequest.getDomainName());
      if (request.getPrefix().equals("@") && request.getRecordType() == A) {
        assertThat(request.isOk()).isTrue();
        assertThat(request.getProblem()).isNull();
        assertThat(request.getRcode()).isEqualTo(0);
        assertThat(request.getResponses()).hasSize(1);
        assertThat(request.getNumOfResponses()).isEqualTo(1);
        assertThat(request.getResponses().get(0).getRecordData()).isEqualTo(IP1);
      } else {
        assertThat(request.isOk()).isFalse();
        assertThat(request.getProblem()).isEqualTo("nxdomain");
        assertThat(request.getRcode()).isEqualTo(Rcode.NXDOMAIN);
        assertThat(request.getResponses()).isEmpty();
        assertThat(request.getNumOfResponses()).isEqualTo(0);
      }
    }
    assertThat(requests).hasSize(10);
    verify(enricher, times(1)).enrichResponses(any());
  }
}

