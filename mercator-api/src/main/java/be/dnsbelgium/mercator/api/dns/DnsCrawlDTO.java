package be.dnsbelgium.mercator.api.dns;

import lombok.Data;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;

@Data
public class DnsCrawlDTO { // WIP

    private long id;
    private List<Integer> rcode;
    private boolean ok;
    private String problem;
    private ZonedDateTime crawlTimestamp;
    private Map<String, String> prefixAndRecordType;
    private Map<String, String> recordTypeAndData;
    private Map<String, Integer> ttl;
    private List<GeoIpDTO> geoIps;

}
