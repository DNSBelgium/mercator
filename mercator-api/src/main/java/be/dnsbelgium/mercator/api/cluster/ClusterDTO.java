package be.dnsbelgium.mercator.api.cluster;

import lombok.Data;

import java.util.UUID;

@Data
public class ClusterDTO {

    private String receivedVisitId;
    private UUID visitId;
    private String domainName;
    private String screenshotKey;

}
