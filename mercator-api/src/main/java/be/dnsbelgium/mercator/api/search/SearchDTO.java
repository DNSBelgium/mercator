package be.dnsbelgium.mercator.api.search;

import be.dnsbelgium.mercator.api.status.CrawlComponentStatus;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.UUID;

@Data // Lombok annotation to generate constructor, getters, setters, toString, hashCode & equals methods.
public class SearchDTO {

    private UUID visitId;
    private String finalUrl;
    private ZonedDateTime requestTimeStamp;
    private CrawlComponentStatus crawlStatus;

}
