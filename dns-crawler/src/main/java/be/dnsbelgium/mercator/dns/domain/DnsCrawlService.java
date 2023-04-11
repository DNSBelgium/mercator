package be.dnsbelgium.mercator.dns.domain;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.common.messaging.idn.IdnException;
import be.dnsbelgium.mercator.dns.DnsCrawlerConfigurationProperties;
import be.dnsbelgium.mercator.dns.domain.resolver.DnsResolver;
import be.dnsbelgium.mercator.dns.dto.DnsRequest;
import be.dnsbelgium.mercator.dns.dto.RRecord;
import be.dnsbelgium.mercator.dns.dto.RecordType;
import be.dnsbelgium.mercator.dns.persistence.Request;
import be.dnsbelgium.mercator.dns.persistence.RequestRepository;
import be.dnsbelgium.mercator.dns.persistence.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

import javax.transaction.Transactional;
import java.net.IDN;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static be.dnsbelgium.mercator.dns.dto.RecordType.A;

@Component
public class DnsCrawlService {
  private static final Logger logger = LoggerFactory.getLogger(DnsCrawlService.class);
  private final RequestRepository requestRepository;
  private final DnsResolver resolver;
  private final Enricher enricher;
  private final DnsCrawlerConfigurationProperties dnsCrawlerConfig;

  public DnsCrawlService(RequestRepository requestRepository, DnsResolver resolver, Enricher enricher, DnsCrawlerConfigurationProperties dnsCrawlerConfig) {
    this.requestRepository = requestRepository;
    this.resolver = resolver;
    this.enricher = enricher;
    this.dnsCrawlerConfig = dnsCrawlerConfig;
  }

  @Transactional
  public void retrieveDnsRecords(VisitRequest visitRequest) {
    String a_label, u_label;
    try {
      a_label = visitRequest.a_label();
      u_label = visitRequest.u_label();
    } catch (IdnException e) {
      logger.error("VisitRequest contains invalid domain name [{}] => skipping this request", visitRequest.getDomainName());
      return;
    }
    if (u_label.equals(a_label)) {
      logger.info("retrieveDnsRecords for {}", u_label);
    } else {
      logger.info("retrieveDnsRecords for {} (a-label: {})", u_label, a_label);
    }
    Name domainName = parseDomainName(a_label);
    if (domainName == Name.empty) {
      return;
    }
    logger.debug("retrieveDnsRecords for [{}]", domainName);

    // First perform a lookup for A records
    DnsRequest initialDnsRequest = resolver.lookup("@", domainName, A);
    int rcode = initialDnsRequest.rcode();
    if (rcode == Lookup.UNRECOVERABLE || rcode == Lookup.TRY_AGAIN) {
      logger.debug("Problem during resolution for {}, dnsjava told us: {}", domainName, initialDnsRequest.humanReadableProblem());
    }
    if (rcode == Lookup.HOST_NOT_FOUND) {
      logger.debug("Resolution for {} resulted in nxdomain. Skipping other record types", domainName);
    }

    List<Request> requests = new ArrayList<>();
    Request initial = buildEntity(visitRequest, initialDnsRequest);
    requests.add(initial);

    if (rcode == Lookup.UNRECOVERABLE || rcode == Lookup.TRY_AGAIN || rcode == Lookup.HOST_NOT_FOUND) {
      // If initialDnsRequest is not ok then we save the failed request to the DB, so we know it has been requested.
      logger.debug("Initial request had rcode = {} != 0 => skip other lookups for {}", rcode, domainName);
    } else {
      // Now lookup all configure record types per prefix
      Set<String> prefixes = dnsCrawlerConfig.getSubdomains().keySet();
      for (String prefix : prefixes.stream().sorted().toList()) {
        List<RecordType> recordTypesToCrawl = dnsCrawlerConfig.getSubdomains().get(prefix);
        for (RecordType recordType : recordTypesToCrawl) {
          //noinspection StatementWithEmptyBody
          if (recordType == A && prefix.equals("@")) {
            // we already did this lookup above
          } else {
            logger.debug("calling resolver.lookup(\"{}\", \"{}\", {})", prefix, domainName, recordType);
            DnsRequest dnsRequest = resolver.lookup(prefix, domainName, recordType);
            logger.debug("Result of lookup: {}", dnsRequest);
            Request request = buildEntity(visitRequest, dnsRequest);
            requests.add(request);
          }
        }
      }
    }
    enricher.enrichResponses(requests);
    requestRepository.saveAll(requests);
  }

  public Request buildEntity(VisitRequest visitRequest, DnsRequest dnsRequest) {
    Request request = Request.builder()
            .visitId(visitRequest.getVisitId())
            .domainName(visitRequest.u_label())
            .prefix(dnsRequest.prefix())
            .recordType(dnsRequest.recordType())
            .rcode(dnsRequest.rcode())
            .ok(dnsRequest.isOk())  ///same oke as in crawler result ?
            .problem(dnsRequest.humanReadableProblem())
            .build();
    for (RRecord record: dnsRequest.records()) {
      Response response = Response.builder()
              .recordData(record.getData())
              .ttl(record.getTtl())
              .build();
      request.getResponses().add(response);
    }
    return request;
  }

  private Name parseDomainName(String domainName) {
    try {
      return new Name(IDN.toASCII(domainName));
    } catch (TextParseException e) {
      logger.error("Cannot correctly parse domain name [{}] included in the visit request. Ignoring this visit request", domainName, e);
      return Name.empty;
    }
  }

}
