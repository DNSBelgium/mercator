package be.dnsbelgium.mercator.dns.domain;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.dns.dto.DnsResolution;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.dto.Records;
import be.dnsbelgium.mercator.dns.persistence.DnsCrawlResult;
import be.dnsbelgium.mercator.dns.persistence.DnsCrawlResultRepository;
import be.dnsbelgium.mercator.dns.persistence.GeoIp;
import be.dnsbelgium.mercator.dns.DnsCrawlerConfigurationProperties;
import be.dnsbelgium.mercator.dns.domain.resolver.DnsResolver;
import be.dnsbelgium.mercator.dns.metrics.MetricName;
import be.dnsbelgium.mercator.geoip.GeoIPService;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import java.net.IDN;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Component
public class DnsCrawlService {

  private static final Logger logger = LoggerFactory.getLogger(DnsCrawlService.class);
  private final MeterRegistry meterRegistry;

  private final DnsCrawlResultRepository repository;
  private final DnsResolver resolver;
  private final GeoIPService geoIPService;
  private final DnsCrawlerConfigurationProperties dnsCrawlerConfig;
  private final boolean geoIpEnabled;

  public DnsCrawlService(DnsCrawlResultRepository repository, DnsResolver resolver, MeterRegistry meterRegistry, GeoIPService geoIPService, DnsCrawlerConfigurationProperties dnsCrawlerConfig, @Value("${crawler.dns.geoIP.enabled}") boolean geoIpEnabled) {
    this.repository = repository;
    this.resolver = resolver;
    this.geoIPService = geoIPService;
    this.meterRegistry = meterRegistry;
    this.dnsCrawlerConfig = dnsCrawlerConfig;
    this.geoIpEnabled = geoIpEnabled;
  }

  public void retrieveDnsRecords(VisitRequest request) {
    Name fqdn = parseDomainName(request.getDomainName());
    if (fqdn == null) return;

    // First perform a lookup for A records
    DnsResolution dnsResolution = resolver.performCheck(fqdn);
    if (!dnsResolution.isOk()) {
      // We save the result because the lookup did not succeed.
      repository.save(DnsCrawlResult.of(request, dnsResolution));
      return;
    }

    for (String subdomain : dnsCrawlerConfig.getSubdomains().keySet()) {
      Name name = getNameFromSubdomain(fqdn, subdomain);
      if (name == null) continue;

      // skip lookup already performed like for the check
      List<RecordType> recordTypes = getRecordTypeToCrawl(subdomain, dnsResolution);
      logger.info("Retrieving records {} for domain [{}]", recordTypes, name);

      // TODO move this timer to include all subdomains
      // TODO Remove exception caused by the recordCallable
      Records records = resolver.getAllRecords(name, recordTypes);
      dnsResolution.addRecords(subdomain, records);
    }

    var result = DnsCrawlResult.of(request, dnsResolution);
    if (geoIpEnabled && dnsResolution.isOk()) {
      meterRegistry.timer(MetricName.GEO_ENRICH).record(() -> enrich(result));
    }
    repository.save(result);
  }

  private Name getNameFromSubdomain(Name fqdn, String subdomain) {
    Name name = null;
    try {
      name = Name.fromString(subdomain, fqdn);
    } catch (TextParseException e) {
      logger.error("Something is wrong with the subdomain [{}]. Ignoring it.", subdomain, e);
    }
    return name;
  }

  private Name parseDomainName(String domainName) {
    Name fqdn;
    try {
      fqdn = new Name(IDN.toASCII(domainName));
    } catch (TextParseException e) {
      logger.error("Cannot correctly parse domain name [{}] included in the visit request. Ignoring this visit request", domainName, e);
      return null;
    }
    return fqdn;
  }

  private List<RecordType> getRecordTypeToCrawl(String subdomain, DnsResolution dnsResolution) {
    List<RecordType> recordTypes = dnsCrawlerConfig.getSubdomains().get(subdomain);
    Records records = dnsResolution.getRecords().get(subdomain);
    if (records != null) {
      recordTypes.removeAll(records.getRecords().keySet());
    }
    return recordTypes;
  }

  private void enrich(DnsCrawlResult crawlResult) {
    List<GeoIp> results = new ArrayList<>();
    for (String subdomain : dnsCrawlerConfig.getSubdomains().keySet()) {
      // Only enrich A and AAAA records
      crawlResult.getAllRecords().get(subdomain).get(List.of(RecordType.A, RecordType.AAAA)).forEach((recordType, records) -> {
        for (String aRecord : records) {
          String country = geoIPService.lookupCountry(aRecord).orElse(null);
          Pair<Integer, String> asn = geoIPService.lookupASN(aRecord).orElse(null);
          if (country != null || asn != null) {
            GeoIp result = new GeoIp(recordType, aRecord, country, asn);
            results.add(result);
          }
        }
      });
    }
    crawlResult.addGeoIp(results);
  }

}
