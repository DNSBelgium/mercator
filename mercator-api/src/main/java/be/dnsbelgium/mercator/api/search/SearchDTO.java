package be.dnsbelgium.mercator.api.search;

import be.dnsbelgium.mercator.api.status.CrawlComponentStatus;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data
public class SearchDTO {

    private UUID visitId;
    private ZonedDateTime requestTimeStamp;
    private CrawlComponentStatus crawlStatus;
    private String domainName;
    private String screenshotKey;
    private String receivedVisitId;

}
