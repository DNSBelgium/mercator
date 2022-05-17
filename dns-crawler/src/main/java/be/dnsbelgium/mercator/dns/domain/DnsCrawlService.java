package be.dnsbelgium.mercator.dns.domain;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.dns.domain.geoip.GeoIpEnricher;
import be.dnsbelgium.mercator.dns.dto.DnsResolution;
import be.dnsbelgium.mercator.dns.dto.RRecord;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.dto.Records;
import be.dnsbelgium.mercator.dns.DnsCrawlerConfigurationProperties;
import be.dnsbelgium.mercator.dns.domain.resolver.DnsResolver;
import be.dnsbelgium.mercator.dns.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import java.net.IDN;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

@Component
public class DnsCrawlService {

  private static final Logger logger = LoggerFactory.getLogger(DnsCrawlService.class);

  private final RequestRepository requestRepository;
  private final DnsResolver resolver;
  private final GeoIpEnricher geoIpEnricher;
  private final DnsCrawlerConfigurationProperties dnsCrawlerConfig;

  public DnsCrawlService(RequestRepository requestRepository, DnsResolver resolver, GeoIpEnricher geoIpEnricher, DnsCrawlerConfigurationProperties dnsCrawlerConfig) {
    this.requestRepository = requestRepository;
    this.resolver = resolver;
    this.geoIpEnricher = geoIpEnricher;
    this.dnsCrawlerConfig = dnsCrawlerConfig;
  }

  // TODO Extract that method to a class and test it
  private List<Request> dnsResolutionToEntity(VisitRequest visitRequest, DnsResolution dnsResolution) {
    List<Request> requests = new ArrayList<>();
    for (String prefix : dnsResolution.getRecords().keySet()) {
      for (RecordType recordType : dnsResolution.getRecords(prefix).getRecords().keySet()) {
        Request request = Request.builder()
            .visitId(visitRequest.getVisitId())
            .domainName(visitRequest.getDomainName())
            .prefix(prefix)
            .recordType(recordType)
            .rcode(dnsResolution.getRcode())
            .ok(dnsResolution.isOk())
            .problem(dnsResolution.getHumanReadableProblem())
            .build();

        // Response
        for (RRecord recordValue : dnsResolution.getRecords(request.getPrefix()).get(request.getRecordType())) {
          Response response = Response.builder()
              .recordData(recordValue.getData())
              .ttl(recordValue.getTtl())
              .build();
          request.getResponses().add(response);

          // Geo IPs
          if (Arrays.asList(RecordType.A, RecordType.AAAA, RecordType.NS).contains(recordType)) {
            try {
              InetAddress[] ips = InetAddress.getAllByName(response.getRecordData());
              for (InetAddress ip : ips) {
                response.getResponseGeoIps().add(geoIpEnricher.enrich(ip));
              }
            } catch (UnknownHostException e) {
              logger.warn("IP isn't parseable");
            }
          }
        }

        request.setNumOfResponses(request.getResponses().size());
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

}
