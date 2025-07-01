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
    private final Instant crawlStarted;
    private final Instant crawlFinished;
    private final String visitId;

    @Builder
    public DnsCrawlResult(List<Request> requests, CrawlStatus status, String domainName, Instant crawlStarted, Instant crawlFinished, String visitId) {
        this.requests = requests != null ? requests : Collections.emptyList();
        this.status = status;
        this.domainName = domainName;
        this.crawlStarted = crawlStarted;
        this.crawlFinished = crawlFinished;
        this.visitId = visitId;
    }

}
