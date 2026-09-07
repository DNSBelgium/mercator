package be.dnsbelgium.mercator.pipeline.service;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class VisitRequest {

    private String visitId;
    private String domainName;

}
