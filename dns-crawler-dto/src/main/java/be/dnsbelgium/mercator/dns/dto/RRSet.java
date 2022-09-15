package be.dnsbelgium.mercator.dns.dto;

import java.util.Set;

public record RRSet(Set<RRecord> records, Integer rcode) {}
