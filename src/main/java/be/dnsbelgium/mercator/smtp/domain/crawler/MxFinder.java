package be.dnsbelgium.mercator.smtp.domain.crawler;

import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xbill.DNS.*;
import org.xbill.DNS.Record;

import java.net.IDN;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class MxFinder {

  private final ExtendedResolver resolver;
  private static final Logger logger = getLogger(MxFinder.class);

  @Autowired
  public MxFinder(
      @Value("${smtp.crawler.recursive.resolver.hostName:8.8.8.8}") String recursiveResolver,
      @Value("${smtp.crawler.recursive.resolver.retries:2}") int retries,
      @Value("${smtp.crawler.recursive.resolver.timeOut.ms:2500}") int timeOutMs,
      @Value("${smtp.crawler.recursive.resolver.tcp.by.default:false}") boolean tcp
  )
      throws UnknownHostException {
    logger.info("Initializing MxFinder with recursiveResolver hostname={} retries={} timeOut={}ms tcp={}",
        recursiveResolver, retries, timeOutMs, tcp);

    var simple = new SimpleResolver(recursiveResolver);
    resolver = new ExtendedResolver(new Resolver[]{simple});
    resolver.setRetries(retries);
    resolver.setTimeout(Duration.ofMillis(timeOutMs));
    resolver.setTCP(tcp);
  }

  public MxLookupResult findMxRecordsFor(String domainName) {
    logger.debug("Finding MX records for [{}]", domainName);
    Name owner;
    try {
      String aLabel = IDN.toASCII(domainName);
      owner = Name.fromString(aLabel);
    } catch (IllegalArgumentException | TextParseException e) {
      logger.error("Invalid name: [{}] : {}", domainName, e.getMessage());
      return MxLookupResult.invalidHostName();
    }
    Lookup lookup = new Lookup(owner, Type.MX);
    lookup.setResolver(resolver);
    lookup.run();
    int rcode = lookup.getResult();
    logger.debug("MX lookup for {} => rcode = {} = {}", domainName, rcode, Rcode.string(rcode));
    if (rcode == Rcode.NXDOMAIN) {
      logger.warn("Domain {} does not exist: {}", domainName, lookup.getErrorString());
      // We do not return NO_MX_RECORDS_FOUND in this case since the domain does not exist => there is no point to search for address records
      // In the context of crawling the domain name is thus invalid (it does not exist according to DNS)
      // => there is no point to search for address records
      return MxLookupResult.invalidHostName();
    }
    if (rcode == Rcode.SERVFAIL) {
      logger.info("Query for {} failed: {}", domainName, lookup.getErrorString());
      return MxLookupResult.queryFailed();
    }
    if (rcode != Rcode.NOERROR) {
      // The rcode returned by dnsjava does not match the rcode received from the server (and the rcode seen with dig)
      // For some reason dnsjava returns rcode = 4 = NOT_IMPL when the domain exists but has no MX records
      // => just log it, but do not fail
      // if the answer contains MX records that's fine, if it doesn't that's fine too
      logger.debug("MX lookup for {} => rcode = {} = {}.  error={}", domainName, rcode, Rcode.string(rcode), lookup.getErrorString());
    }
    Record[] answers = lookup.getAnswers();
    if (answers == null || answers.length == 0) {
      logger.debug("No MX records found for {}", domainName);
      return MxLookupResult.noMxRecordsFound();
    }
    logger.debug("We found {} records for {}", answers.length, domainName);
    List<MXRecord> mxRecords = new ArrayList<>();
    for (Record answer : answers) {
      if (answer instanceof MXRecord mxRecord) {
        logger.debug("mxRecord = {}", mxRecord);
        mxRecords.add(mxRecord);
      } else {
        // Let's log it, so we can see if it happens often.
        // Some resolvers add TLSA records to the answer when querying for MX records
        logger.debug("Skipping {} since it's not an MxRecord", answer);
      }
    }
    return MxLookupResult.ok(mxRecords);
  }


  public List<InetAddress> findIpAddresses(String hostName) {
    logger.debug("Finding IP addresses for {}", hostName);
    try {
      InetAddress[] addresses = InetAddress.getAllByName(hostName);
      return Arrays.asList(addresses);
    } catch (UnknownHostException e) {
      logger.info("Unknown host: {}", hostName);
      return Collections.emptyList();
    }
  }
}
