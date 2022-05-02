package be.dnsbelgium.mercator.dns.domain;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.dns.dto.DnsResolution;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.dto.Records;
import be.dnsbelgium.mercator.dns.DnsCrawlerConfigurationProperties;
import be.dnsbelgium.mercator.dns.domain.resolver.DnsResolver;
import be.dnsbelgium.mercator.dns.metrics.MetricName;
import be.dnsbelgium.mercator.dns.persistence.*;
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
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Component
public class DnsCrawlService {

  private static final Logger logger = LoggerFactory.getLogger(DnsCrawlService.class);
  private final MeterRegistry meterRegistry;

  private final RequestRepository requestRepository;
  private final ResponseRepository responseRepository;
  private final ResponseGeoIpRepository responseGeoIpRepository;
  private final DnsResolver resolver;
  private final GeoIPService geoIPService;
  private final DnsCrawlerConfigurationProperties dnsCrawlerConfig;
  private final boolean geoIpEnabled;

  public DnsCrawlService(RequestRepository requestRepository, ResponseRepository responseRepository, ResponseGeoIpRepository responseGeoIpRepository, DnsResolver resolver, MeterRegistry meterRegistry, GeoIPService geoIPService, DnsCrawlerConfigurationProperties dnsCrawlerConfig, @Value("${crawler.dns.geoIP.enabled}") boolean geoIpEnabled) {
    this.requestRepository = requestRepository;
    this.responseRepository = responseRepository;
    this.responseGeoIpRepository = responseGeoIpRepository;
    this.resolver = resolver;
    this.geoIPService = geoIPService;
    this.meterRegistry = meterRegistry;
    this.dnsCrawlerConfig = dnsCrawlerConfig;
    this.geoIpEnabled = geoIpEnabled;
  }

  public void retrieveDnsRecords(VisitRequest visitRequest) {
    Name fqdn = parseDomainName(visitRequest.getDomainName());
    if (fqdn == null) return;

    // First perform a lookup for A records
    DnsResolution dnsResolution = resolver.performCheck(fqdn);

    // Creating a List of the prefixes for easier use.
    Set<String> prefixSet = dnsCrawlerConfig.getSubdomains().keySet();
    List<String> prefixes = new ArrayList<>(prefixSet);

    // If dnsResolution is not ok then we save the failed request to the DB, so we know it has been requested.
    if (!dnsResolution.isOk()) {
      Request request = Request.builder()
              .visitId(visitRequest.getVisitId())
              .domainName(visitRequest.getDomainName())
              .prefix("@")
              .recordType(RecordType.A)
              .rcode(3) // TODO: AvR get actual rcode?
              .crawlTimestamp(ZonedDateTime.now())
              .ok(false)
              .problem(dnsResolution.getHumanReadableProblem())
              .build();
      requestRepository.save(request);
      return;
    }

    for (String prefix : prefixes) {
      Name name = getNameFromSubdomain(fqdn, prefix);
      if (name == null) continue;

      List<RecordType> recordTypes = getRecordTypeToCrawl(prefix, dnsResolution);
      logger.info("Retrieving records {} for domain [{}]", recordTypes, name);

      // TODO move this timer to include all subdomains
      // TODO Remove exception caused by the recordCallable

      Records records = resolver.getAllRecords(name, recordTypes);
      DnsResolution resolution = dnsResolution.addRecords(prefix, records);

      // For each recordType found in resolution we create a new Request.
      // For each recordValue found in recordType we create a new Response.
      // For each RecordType.A or RecordType.AAAA we create a new ResponseGeoIp (enrich method).
      for (RecordType recordType: resolution.getRecords(prefix).getRecords().keySet()) {
        Request request = Request.builder()
                .visitId(visitRequest.getVisitId())
                .domainName(visitRequest.getDomainName())
                .prefix(prefix)
                .recordType(recordType)
                .rcode(0) // TODO: AvR get actual rcode?
                .crawlTimestamp(ZonedDateTime.now())
                .ok(true)
                .problem(dnsResolution.getHumanReadableProblem())
                .build();

        Optional<Request> optionalRequest = Optional.of(requestRepository.save(request));
        Request savedRequest = optionalRequest.get(); //TODO: Improve.

        for (String recordValue: resolution.getRecords(prefix).get(recordType)) {
          Response response = Response.builder()
                  .recordData(recordValue)
                  .ttl(0) // TODO: AvR get TTL
                  .request(savedRequest)
                  .build();

          Optional<Response> optionalResponse = Optional.of(responseRepository.save(response));
          Response savedResponse = optionalResponse.get();

          if (geoIpEnabled && dnsResolution.isOk()) {
            if (recordType == RecordType.A) {
              meterRegistry.timer(MetricName.GEO_ENRICH).record(() -> enrich(savedResponse, 4));
            } else if (recordType == RecordType.AAAA) {
              meterRegistry.timer(MetricName.GEO_ENRICH).record(() -> enrich(savedResponse, 6));
            }
          }
        }
      }
    }
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

  private List<RecordType> getRecordTypeToCrawl(String prefix, DnsResolution dnsResolution) {
    List<RecordType> recordTypes = dnsCrawlerConfig.getSubdomains().get(prefix);
    Records records = dnsResolution.getRecords().get(prefix);
    if (records != null) {
      recordTypes.removeAll(records.getRecords().keySet());
    }
    return recordTypes;
  }

  private void enrich(Response response, int ipVersion) {
    String country = geoIPService.lookupCountry(response.getRecordData()).orElse(null);
    Pair<Integer, String> asn = geoIPService.lookupASN(response.getRecordData()).orElse(null);

    if (country != null || asn != null) {
      ResponseGeoIp result = new ResponseGeoIp(asn, country, ipVersion, response);
      responseGeoIpRepository.save(result);
    }
  }



}
