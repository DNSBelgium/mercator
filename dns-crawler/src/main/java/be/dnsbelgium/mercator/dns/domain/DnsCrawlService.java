package be.dnsbelgium.mercator.dns.domain;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.dns.domain.geoip.GeoIpEnricher;
import be.dnsbelgium.mercator.dns.dto.*;
import be.dnsbelgium.mercator.dns.DnsCrawlerConfigurationProperties;
import be.dnsbelgium.mercator.dns.domain.resolver.DnsResolver;
import be.dnsbelgium.mercator.dns.persistence.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

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
  private List<Request> dnsResolutionToEntity(UUID visitId, DnsResolution dnsResolution) {
    List<Request> requests = new ArrayList<>();
    for (String prefix : dnsResolution.getRecords().keySet()) {
      for (RecordType recordType : dnsResolution.getRecords(prefix).getRecords().keySet()) {
        RRSet rrSet = dnsResolution.getRecords(prefix).get(recordType);
        Request request = Request.builder()
            .visitId(visitId)
            .domainName(dnsResolution.getDomainName())
            .prefix(prefix)
            .recordType(recordType)
            .rcode(rrSet.rcode())
            .ok(dnsResolution.isOk())
            .problem(dnsResolution.getHumanReadableProblem())
            .build();

        // Response
        for (RRecord record : rrSet.records()) {
          Response response = Response.builder()
              .recordData(record.data())
              .ttl(record.ttl())
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

        requests.add(request);
      }
    }
    return requests;
  }

  public void retrieveDnsRecords(VisitRequest visitRequest) {
    String domainName = visitRequest.getDomainName();
    Map<String, List<RecordType>> recordTypesPerPrefix = dnsCrawlerConfig.getSubdomains();

    for (String prefix : recordTypesPerPrefix.keySet()) {
      List<RecordType> recordTypes = recordTypesPerPrefix.get(prefix);
      logger.info("Retrieving records {} for domain [{}] and prefix [{}]", recordTypes, domainName, prefix);

      DnsResolution dnsResolution = resolver.getAllRecords(domainName, prefix, new HashSet<>(recordTypes));
      requestRepository.saveAll(dnsResolutionToEntity(visitRequest.getVisitId(), dnsResolution));
    }
  }

}
