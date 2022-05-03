package be.dnsbelgium.mercator.dns.domain;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.dns.dto.DnsResolution;
import be.dnsbelgium.mercator.dns.dto.RRecord;
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
import java.util.*;

@Component
public class DnsCrawlService {

  private static final Logger logger = LoggerFactory.getLogger(DnsCrawlService.class);
  private final MeterRegistry meterRegistry;

  private final RequestRepository requestRepository;
  private final DnsResolver resolver;
  private final GeoIPService geoIPService;
  private final DnsCrawlerConfigurationProperties dnsCrawlerConfig;
  private final boolean geoIpEnabled;

  public DnsCrawlService(RequestRepository requestRepository, DnsResolver resolver, MeterRegistry meterRegistry, GeoIPService geoIPService, DnsCrawlerConfigurationProperties dnsCrawlerConfig, @Value("${crawler.dns.geoIP.enabled}") boolean geoIpEnabled) {
    this.requestRepository = requestRepository;
    this.resolver = resolver;
    this.geoIPService = geoIPService;
    this.meterRegistry = meterRegistry;
    this.dnsCrawlerConfig = dnsCrawlerConfig;
    this.geoIpEnabled = geoIpEnabled;
  }

  private List<Request> dnsResolutionToEntity(VisitRequest visitRequest, DnsResolution dnsResolution) {
    ArrayList<Request> requests = new ArrayList();
    for (Map.Entry<String, Records> recordsPerRecordType : dnsResolution.getRecords().entrySet()) {
      for (Map.Entry<RecordType, List<RRecord>> records: recordsPerRecordType.getValue().getRecords().entrySet()) {
        Request request = Request.builder()
            .visitId(visitRequest.getVisitId())
            .domainName(visitRequest.getDomainName())
            .prefix(recordsPerRecordType.getKey())
            .recordType(records.getKey())
            .rcode(dnsResolution.getRcode())
            .ok(false)
            .problem(dnsResolution.getHumanReadableProblem())
            .build();

        // Response
        for (RRecord recordValue: dnsResolution.getRecords(request.getPrefix()).get(request.getRecordType())) {
          Response response = Response.builder()
              .recordData(recordValue.getData())
              .ttl(recordValue.getTtl())
              .build();
          request.getResponses().add(response);

        // Geo IPs
        if (geoIpEnabled) {
            if (request.getRecordType() == RecordType.A) {
              meterRegistry.timer(MetricName.GEO_ENRICH).record(() -> enrich(response, 4));
            } else if (request.getRecordType() == RecordType.AAAA) {
              meterRegistry.timer(MetricName.GEO_ENRICH).record(() -> enrich(response, 6));
            }
          }
        }

        requests.add(request);
      }
    }
    return requests;
  }

  public void retrieveDnsRecords(VisitRequest visitRequest) {
    Name fqdn = parseDomainName(visitRequest.getDomainName());
    if (fqdn == null) return;

    // First perform a lookup for A records
    DnsResolution dnsResolution = resolver.performCheck(fqdn);

    // Creating a List of the prefixes for easier use.
    Set<String> prefixes = dnsCrawlerConfig.getSubdomains().keySet();

    // If dnsResolution is not ok then we save the failed request to the DB, so we know it has been requested.
    if (!dnsResolution.isOk()) {
      requestRepository.saveAll(dnsResolutionToEntity(visitRequest, dnsResolution));
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
      dnsResolution.addRecords(prefix, records);
    }

    requestRepository.saveAll(dnsResolutionToEntity(visitRequest, dnsResolution));
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
      ResponseGeoIp result = new ResponseGeoIp(asn, country, ipVersion, response.getRecordData());
      response.getResponseGeoIps().add(result);
    }
  }
}
