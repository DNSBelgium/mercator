package be.dnsbelgium.mercator.dns;

import be.dnsbelgium.mercator.dns.dto.RecordType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class DnsCrawlerConfigurationPropertiesTest {

  @Autowired
  DnsCrawlerConfigurationProperties dnsCrawlerConfig;

  @Test
  void apexRecordTypesIncludeCdnskey() {
    List<RecordType> apexRecordTypes = dnsCrawlerConfig.getSubdomains().get("@");
    assertThat(apexRecordTypes).isNotNull();
    // CDNSKEY (together with DNSKEY and CDS) must be queried for the apex/root domain
    // so we can measure the DNSSEC key rollover / DS record automation of a domain
    assertThat(apexRecordTypes).contains(RecordType.CDNSKEY, RecordType.DNSKEY, RecordType.CDS);
  }
}
