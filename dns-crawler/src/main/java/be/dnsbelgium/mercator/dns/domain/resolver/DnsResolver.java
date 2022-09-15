package be.dnsbelgium.mercator.dns.domain.resolver;

import be.dnsbelgium.mercator.dns.dto.DnsResolution;
import be.dnsbelgium.mercator.dns.dto.RecordType;

import java.util.Set;

public interface DnsResolver {
  DnsResolution getAllRecords(String domainName, String prefix, Set<RecordType> recordTypes);

}
