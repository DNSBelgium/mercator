package be.dnsbelgium.mercator.smtp.domain.crawler;

import be.dnsbelgium.mercator.smtp.TxLogger;
import be.dnsbelgium.mercator.smtp.metrics.MetricName;
import be.dnsbelgium.mercator.smtp.persistence.entities.CrawlStatus;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversationEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHostEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisitEntity;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xbill.DNS.MXRecord;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Analyzes the SMTP servers for a given domain, by retrieving all MX records
 * and talking with all corresponding SMTP servers
 */
@Component
public class SmtpAnalyzer {

  private final MeterRegistry meterRegistry;
  private final SmtpIpAnalyzer smtpIpAnalyzer;
  private final MxFinder mxFinder;
  private final boolean skipIPv4;
  private final boolean skipIPv6;

  private final SmtpConversationCache conversationCache;

  private static final Logger logger = getLogger(SmtpAnalyzer.class);

  @Autowired
  public SmtpAnalyzer(MeterRegistry meterRegistry, SmtpIpAnalyzer smtpIpAnalyzer, MxFinder mxFinder,
                      SmtpConversationCache smtpConversationCache,
                      @Value("${smtp.crawler.skip.ipv4:false}") boolean skipIPv4, @Value("${smtp.crawler.skip.ipv6:false}") boolean skipIPv6) {
    this.meterRegistry = meterRegistry;
    this.smtpIpAnalyzer = smtpIpAnalyzer;
    this.mxFinder = mxFinder;
    this.skipIPv4 = skipIPv4;
    this.skipIPv6 = skipIPv6;
    this.conversationCache = smtpConversationCache;
    logger.info("skipIPv4={} skipIPv6={}", skipIPv4, skipIPv6);
  }

  public SmtpVisitEntity analyze(String domainName) throws Exception {
    TxLogger.log(getClass(), "analyze");
    SmtpVisitEntity result = meterRegistry.timer(MetricName.TIMER_SMTP_ANALYSIS).recordCallable(() -> doCrawl(domainName));
    meterRegistry.counter(MetricName.SMTP_DOMAINS_DONE).increment();
    return result;
  }

  private SmtpVisitEntity doCrawl(String domainName) {
    TxLogger.log(getClass(), "doCrawl");
    logger.debug("Starting SMTP crawl for domainName={}", domainName);
    SmtpVisitEntity result = new SmtpVisitEntity();
    result.setDomainName(domainName);
    result.setTimestamp(ZonedDateTime.now());
    MxLookupResult mxLookupResult = mxFinder.findMxRecordsFor(domainName);
    switch (mxLookupResult.getStatus()) {
      case INVALID_HOSTNAME -> {
        result.setCrawlStatus(CrawlStatus.INVALID_HOSTNAME);
        meterRegistry.counter(MetricName.COUNTER_INVALID_HOSTNAME).increment();
        return result;
      }
      case QUERY_FAILED -> {
        result.setCrawlStatus(CrawlStatus.NETWORK_ERROR);
        meterRegistry.counter(MetricName.COUNTER_NETWORK_ERROR).increment();
        return result;
      }
      case NO_MX_RECORDS_FOUND -> {
        visitAddressRecords(result);
        return result;
      }
      case OK -> {
        visitMxRecords(result, mxLookupResult);
        return result;
      }
      default -> throw new RuntimeException("Unknown MxLookupResult Status");
    }
  }

  private void visitAddressRecords(SmtpVisitEntity visit) {
    // CNAME-s are followed when resolving hostnames to addresses
    // It seems that CNAME's are also followed hen looking up MX records
    //
    //    https://tools.ietf.org/html/rfc5321#section-5.1
    //
    //   The lookup first attempts to locate an MX record associated with the
    //   name.  If a CNAME record is found, the resulting name is processed as
    //   if it were the initial name.  If a non-existent domain error is
    //   returned, this situation MUST be reported as an error.  If a
    //   temporary error is returned, the message MUST be queued and retried
    //   later (see Section 4.5.4.1).  If an empty list of MXs is returned,
    //   the address is treated as if it was associated with an implicit MX
    //   RR, with a preference of 0, pointing to that host.  If MX records are
    //   present, but none of them are usable, or the implicit MX is unusable,
    //   this situation MUST be reported as an error.
    String domainName = visit.getDomainName();
    meterRegistry.counter(MetricName.COUNTER_NO_MX_RECORDS_FOUND).increment();
    logger.debug("No MX records found for {} => finding address records", domainName);
    List<SmtpHostEntity> hosts = visit(domainName, 0, false);
    visit.add(hosts);
    setStatus(visit);
    logger.debug("DONE crawling A records for domain name {}", domainName);
  }

  private void visitMxRecords(SmtpVisitEntity visit, MxLookupResult mxLookupResult) {
    String domainName = visit.getDomainName();
    logger.debug("We found {} MX records for {}", mxLookupResult.getMxRecords().size(), domainName);
    for (MXRecord mxRecord : mxLookupResult.getMxRecords()) {
      logger.debug("mxRecord = {}", mxRecord);
      String hostName = mxRecord.getTarget().toString(true);
      List<SmtpHostEntity> hosts = visit(hostName, mxRecord.getPriority(), true);
      visit.add(hosts);
    }
    setStatus(visit);
    logger.debug("DONE crawling MX records for domain name {}", domainName);
  }

  private void setStatus(SmtpVisitEntity visit) {
    if (visit.getHosts().stream().anyMatch(host -> host
        .getConversation()
        .getError() == null)) {
      visit.setCrawlStatus(CrawlStatus.OK);
    } else {
      visit.setCrawlStatus(CrawlStatus.NO_REACHABLE_SMTP_SERVERS);
    }
  }

  private List<SmtpHostEntity> visit(String hostName, int priority, boolean fromMx) {
    List<InetAddress> addresses = mxFinder.findIpAddresses(hostName);
    if (addresses.size() == 0) {
      logger.debug("No addresses found for hostName {}", hostName);
      return Collections.emptyList();
    }
    logger.debug("We found {} addresses for hostName {}", addresses.size(), hostName);
    List<SmtpHostEntity> hosts = new ArrayList<>();
    for (InetAddress address : addresses) {
      SmtpConversationEntity smtpConversation = findInCacheOrCrawl(address);
      smtpConversation.clean();
      SmtpHostEntity host = new SmtpHostEntity();
      host.setHostName(hostName);
      host.setPriority(priority);
      host.setConversation(smtpConversation);
      host.setFromMx(fromMx);
      if (!fromMx) {
        host.setHostName(smtpConversation.getIp());
      }
      hosts.add(host);
    }
    return hosts;
  }

  private SmtpConversationEntity findInCacheOrCrawl(InetAddress address) {
    logger.debug("crawling ip {}", address.toString());

    if (address.isLoopbackAddress()) {
      return skip(address, "conversation with loopback address skipped");
    }
    if (address.isSiteLocalAddress()) {
      return skip(address, "conversation with site local address skipped");
    }
    if (skipIPv4 && address instanceof Inet4Address) {
      return skip(address, "conversation with IPv4 SMTP host skipped");
    }
    if (skipIPv6 && address instanceof Inet6Address) {
      return skip(address, "conversation with IPv6 SMTP host skipped");
    }

    // first check the cache
    String ip = address.getHostAddress();
    SmtpConversationEntity conversation = conversationCache.get(ip);
    if (conversation != null) {
      logger.debug("Found conversation in the cache: {}", conversation);
    } else {
      conversation = smtpIpAnalyzer.crawl(address);
      logger.debug("done crawling ip {}", address);
    }
    return conversation;

  }

  private SmtpConversationEntity skip(InetAddress address, String message) {
    meterRegistry.counter(MetricName.COUNTER_ADDRESSES_SKIPPED, Tags.of("reason", message)).increment();
    logger.debug("{} : {}", message, address);
    SmtpConversationEntity conversation = new SmtpConversationEntity(address);
    conversation.setErrorMessage(message);
    return conversation;
  }

}
