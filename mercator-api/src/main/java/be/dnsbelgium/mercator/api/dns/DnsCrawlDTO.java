package be.dnsbelgium.mercator.api.dns;

import be.dnsbelgium.mercator.dns.persistence.ResponseGeoIp;
import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Data
public class DnsCrawlDTO {

    private long id;
    private boolean ok;
    private String problem;
    private ZonedDateTime crawlTimestamp;
    private Map<String, List<RecordWrapper>> prefixAndData;
    private List<ResponseGeoIp> geoIps;

}
