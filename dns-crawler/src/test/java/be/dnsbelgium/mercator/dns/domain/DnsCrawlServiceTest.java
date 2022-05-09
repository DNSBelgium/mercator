package be.dnsbelgium.mercator.dns.domain;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.dns.domain.geoip.GeoIpEnricher;
import be.dnsbelgium.mercator.dns.dto.DnsResolution;
import be.dnsbelgium.mercator.dns.dto.RRecord;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.dto.Records;
import be.dnsbelgium.mercator.dns.DnsCrawlerConfigurationProperties;
import be.dnsbelgium.mercator.dns.domain.resolver.*;
import be.dnsbelgium.mercator.dns.persistence.Request;
import be.dnsbelgium.mercator.dns.persistence.RequestRepository;
import be.dnsbelgium.mercator.dns.persistence.Response;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.xbill.DNS.Name;

import java.util.*;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig({DnsCrawlService.class, MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
@TestPropertySource(properties = {"crawler.dns.geoIP.enabled=true"})
class DnsCrawlServiceTest {

  @MockBean
  RequestRepository requestRepository;

  @MockBean
  DnsResolver dnsResolver;

  @MockBean
  GeoIpEnricher geoIpEnricher;

  @MockBean
  DnsCrawlerConfigurationProperties dnsCrawlerConfig;

  @Autowired
  DnsCrawlService dnsCrawlService;

  @Captor
  ArgumentCaptor<List<Request>> argCaptor;

  @Value("${crawler.dns.geoIP.enabled}")
  boolean geoIpEnabled;

  @Test
  void geoIpShouldBeEnabled() {
    assertThat(geoIpEnabled).isTrue();
  }

  @Test
  void retrieveDnsRecordsDomainNotFound() {
    when(dnsResolver.performCheck(any(Name.class))).thenReturn(DnsResolution.nxdomain().addRecords("@", new Records(Map.of(RecordType.A, Collections.emptyList()))));

    VisitRequest visitRequest = new VisitRequest(UUID.randomUUID(), "dnsbelgium.be");

    dnsCrawlService.retrieveDnsRecords(visitRequest);

    verify(requestRepository).saveAll(argCaptor.capture());
    List<Request> requests = argCaptor.getValue();

    assertThat(requests).hasSize(1);
    assertThat(requests.get(0).isOk()).isFalse();
    assertThat(requests.get(0).getProblem()).isEqualTo("nxdomain");
    assertThat(requests.get(0).getResponses()).isEmpty();
  }

  @Test
  void retrieveDnsRecords() {
    when(dnsResolver.performCheck(any(Name.class))).thenReturn(DnsResolutionTest.dnsBelgiumDnsResolution());
    when(dnsResolver.getAllRecords(any(Name.class), anyList())).thenReturn(new Records());
    when(dnsCrawlerConfig.getSubdomains()).thenReturn(new HashMap<>(Map.of(
        "@", new ArrayList<>(List.of(RecordType.SOA, RecordType.A, RecordType.AAAA, RecordType.CAA, RecordType.MX)),
        "www", new ArrayList<>(List.of(RecordType.A, RecordType.AAAA)),
        "_dmarc", new ArrayList<>(List.of(RecordType.TXT))
    )));

    VisitRequest visitRequest = new VisitRequest(UUID.randomUUID(), "dnsbelgium.be");

    dnsCrawlService.retrieveDnsRecords(visitRequest);

    verify(requestRepository).saveAll(argCaptor.capture());

    List<Request> requests = argCaptor.getValue();
    assertThat(requests).hasSize(9);

    DnsResolution dnsResolution = DnsResolutionTest.dnsBelgiumDnsResolution();

    for (String prefix : dnsResolution.getRecords().keySet()) {
      for (RecordType recordType : dnsResolution.getRecords(prefix).getRecords().keySet()) {
        List<Request> collect = requests.stream().filter(request -> request.getPrefix().equals(prefix)).filter(request -> request.getRecordType() == recordType).collect(Collectors.toList());
        assertThat(collect).hasSize(1);
        Request request = collect.get(0);

        assertThat(request.getVisitId()).isEqualTo(visitRequest.getVisitId());
        assertThat(request.getDomainName()).isEqualTo("dnsbelgium.be");
        assertThat(request.getRcode()).isEqualTo(0);
        assertTrue(request.isOk());
        assertThat(request.getProblem()).isNull();

        assertThat(request.getResponses()).hasSize(DnsResolutionTest.dnsBelgiumDnsResolution().getRecords(prefix).get(recordType).size());

        for (Response response : request.getResponses()) {
          assertThat(new RRecord(response.getTtl(), response.getRecordData())).isIn(dnsResolution.getRecords(prefix).getRecords().get(recordType));
        }
      }
    }

    verify(geoIpEnricher, times(4)).enrich(any());
  }
}
