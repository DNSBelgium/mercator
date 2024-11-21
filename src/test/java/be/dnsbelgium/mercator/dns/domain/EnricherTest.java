package be.dnsbelgium.mercator.dns.domain;

import be.dnsbelgium.mercator.dns.domain.geoip.GeoIpEnricher;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.persistence.Request;
import be.dnsbelgium.mercator.dns.persistence.ResponseGeoIp;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
//import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.net.InetAddress;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;


@SuppressWarnings("SpringBootApplicationProperties")
@SpringJUnitConfig({Enricher.class, MetricsAutoConfiguration.class, CompositeMeterRegistryAutoConfiguration.class})
@TestPropertySource(properties = {"crawler.dns.geoIP.enabled=true"})
class EnricherTest {

  // @MockitoBean
  @MockBean
  GeoIpEnricher geoIpEnricher;

  @Autowired Enricher enricher;

  private static final Logger logger = LoggerFactory.getLogger(EnricherTest.class);

  @Test
  public void shouldEnrich() {
    assertThat(enricher.shouldEnrich(of(RecordType.A))).isTrue();
    assertThat(enricher.shouldEnrich(of(RecordType.AAAA))).isTrue();
    assertThat(enricher.shouldEnrich(of(RecordType.NS))).isTrue();
    assertThat(enricher.shouldEnrich(of(RecordType.TXT))).isFalse();
  }

  private static boolean avoidInternet() {
    return true;
  }



  @DisabledIf(value = "avoidInternet", disabledReason="We don't want to rely on the internet during testing")
  @Test
  public void enrich() {
    ResponseGeoIp enriched1 = new ResponseGeoIp(Pair.of(20400L, "Google ASN"), "BE", 4, "1.2.3.4");
    ResponseGeoIp enriched2 = new ResponseGeoIp(Pair.of(20500L, "Google ASN"), "FR", 4, "10.20.30.40");
    when(geoIpEnricher.enrich(any(InetAddress.class)))
            .thenReturn(enriched1)
            .thenReturn(enriched2);
    List<ResponseGeoIp> geoIpList = enricher.enrich("www.google.be");
    logger.info("geoIpList = {}", geoIpList);
    for (ResponseGeoIp geoIp : geoIpList) {
      logger.info("geoIp = {}", geoIp);
      assertThat(geoIp).isIn(enriched1, enriched2);
    }
    assertThat(geoIpList).containsExactly(enriched1, enriched2);
  }

    private Request of(RecordType type) {
    return Request.builder().recordType(type).build();
  }


}