package be.dnsbelgium.mercator.pipeline.dns;

import lombok.Builder;
import lombok.Getter;

/** Placeholder DNS crawl result. Extend with real record data (A/AAAA/MX/…) later. */
@Builder
@Getter
public class DnsResult {

    private String domainName;
    private String visitId;
    private int numRecords;
    private boolean hasMx;
}

