package be.dnsbelgium.mercator.dns.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

@ToString
@Getter
public class DnsCrawlResult {

    private final CrawlStatus status;
    private final List<Request> requests;
    private final String domainName;
    private final Instant crawlTimestamp;
    private final String visitId;

    @Builder
    public DnsCrawlResult(List<Request> requests, CrawlStatus status, String domainName, Instant crawlTimestamp, String visitId) {
        this.requests = requests != null ? requests : Collections.emptyList();
        this.status = status;
        this.domainName = domainName;
        this.crawlTimestamp = crawlTimestamp;
        this.visitId = visitId;
    }

}
