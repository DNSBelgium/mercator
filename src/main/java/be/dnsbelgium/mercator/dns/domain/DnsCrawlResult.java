package be.dnsbelgium.mercator.dns.domain;

import be.dnsbelgium.mercator.dns.CrawlStatus;
import be.dnsbelgium.mercator.dns.persistence.Request;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
@Getter
public class DnsCrawlResult {

    private final CrawlStatus status;
    private final List<Request> requests;

    private DnsCrawlResult(List<Request> requests, CrawlStatus status) {
        this.requests = requests;
        this.status = status;
    }

    public static DnsCrawlResult invalidDomainName() {
        return new DnsCrawlResult(List.of(), CrawlStatus.INVALID_DOMAIN_NAME);
    }

    public static DnsCrawlResult nxdomain(List<Request> requests) {
        return new DnsCrawlResult(requests, CrawlStatus.NXDOMAIN);
    }

    public static DnsCrawlResult of(List<Request> requests) {
        return new DnsCrawlResult(requests, CrawlStatus.OK);
    }

}
