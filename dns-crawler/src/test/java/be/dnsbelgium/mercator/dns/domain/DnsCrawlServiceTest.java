package be.dnsbelgium.mercator.dns.domain;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.dns.dto.DnsResolution;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.dto.Records;
import be.dnsbelgium.mercator.dns.DnsCrawlerConfigurationProperties;
import be.dnsbelgium.mercator.dns.domain.resolver.*;
import be.dnsbelgium.mercator.dns.persistence.Request;
import be.dnsbelgium.mercator.dns.persistence.RequestRepository;
import be.dnsbelgium.mercator.dns.persistence.ResponseGeoIpRepository;
import be.dnsbelgium.mercator.dns.persistence.ResponseRepository;
import be.dnsbelgium.mercator.geoip.GeoIPService;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringJUnitConfig({DnsCrawlService.class, MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
@TestPropertySource(properties = {"crawler.dns.geoIP.enabled=true"})
class DnsCrawlServiceTest {

  @MockBean
  RequestRepository requestRepository;
  @MockBean
  ResponseRepository responseRepository;
  @MockBean
  ResponseGeoIpRepository responseGeoIpRepository;

  @MockBean
  DnsResolver dnsResolver;

  @MockBean
  GeoIPService geoIPService;

  @MockBean
  DnsCrawlerConfigurationProperties dnsCrawlerConfig;

  @Autowired
  DnsCrawlService dnsCrawlService;

  @Captor
  ArgumentCaptor<Request> argCaptor;

  @Value("${crawler.dns.geoIP.enabled}")
  boolean geoIpEnabled;

  @Test
  void geoIpShouldBeEnabled() {
    assertThat(geoIpEnabled).isTrue();
  }

  @Test
  void retrieveDnsRecordsDomainNotFound() {
    when(dnsResolver.performCheck(any(Name.class))).thenReturn(DnsResolution.nxdomain());

    VisitRequest visitRequest = new VisitRequest(UUID.randomUUID(), "dnsbelgium.be");

    dnsCrawlService.retrieveDnsRecords(visitRequest);

    verify(requestRepository).save(argCaptor.capture());
    Request result = argCaptor.getValue();

    assertThat(result.isOk()).isFalse();
    assertThat(result.getProblem()).isEqualTo("nxdomain");
  }

  @Test
  void retrieveDnsRecords() { // TODO: AvR savedRequest is null. (DnsCrawlService 109)
    // If mock calls with those arguments, give X.
    when(dnsResolver.performCheck(any(Name.class))).thenReturn(DnsResolutionTest.dnsBelgiumDnsResolution());
    when(dnsResolver.getAllRecords(any(Name.class), anyList())).thenReturn(new Records());
    when(dnsCrawlerConfig.getSubdomains()).thenReturn(new HashMap<>(Map.of(
        "@", new ArrayList<>(List.of(RecordType.SOA, RecordType.A, RecordType.AAAA, RecordType.CAA, RecordType.MX)),
        "www", new ArrayList<>(List.of(RecordType.A, RecordType.AAAA)),
        "_dmarc", new ArrayList<>(List.of(RecordType.TXT))
    )));

    VisitRequest visitRequest = new VisitRequest(UUID.randomUUID(), "dnsbelgium.be");

    dnsCrawlService.retrieveDnsRecords(visitRequest);

    verify(requestRepository).save(argCaptor.capture());
    verify(geoIPService, times(4)).lookupCountry(anyString());
    verify(geoIPService, times(4)).lookupASN(anyString());
    Request result = argCaptor.getValue();

//    assertThat(result.getAllRecords()).isEqualTo(DnsResolutionTest.dnsBelgiumDnsResolution().getRecords());
    assertThat(result.isOk()).isTrue();
    assertThat(result.getProblem()).isNull();
  }
}
