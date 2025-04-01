package be.dnsbelgium.mercator.dns.dto;

import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.List;

@ToString
@Getter
public class DnsCrawlResult {

    private final CrawlStatus status;
    private final List<Request> requests;
    private final String domainName;
    private final Instant crawlTimestamp;
    private final String visitId;

    public DnsCrawlResult(List<Request> requests, CrawlStatus status, String domainName, Instant crawlTimestamp, String visitId) {
        this.requests = requests;
        this.status = status;
        this.domainName = domainName;
        this.crawlTimestamp = crawlTimestamp;
        this.visitId = visitId;
    }

    public static DnsCrawlResult invalidDomainName() {
        return new DnsCrawlResult(List.of(), CrawlStatus.INVALID_DOMAIN_NAME,null, null, null);
    }

    public static DnsCrawlResult nxdomain(List<Request> requests) {
        return new DnsCrawlResult(requests, CrawlStatus.NXDOMAIN, null,null, null);
    }

    public static DnsCrawlResult of(List<Request> requests) {
        return new DnsCrawlResult(requests, CrawlStatus.OK, null, null, null);
    }


}
