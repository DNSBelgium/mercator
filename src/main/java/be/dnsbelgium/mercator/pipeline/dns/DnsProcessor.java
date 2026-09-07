package be.dnsbelgium.mercator.pipeline.dns;

import be.dnsbelgium.mercator.pipeline.service.ItemProcessor;
import be.dnsbelgium.mercator.pipeline.service.VisitRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Placeholder DNS processor. TODO: perform real DNS lookups. */
@Slf4j
@Component
public class DnsProcessor implements ItemProcessor<VisitRequest, DnsResult> {

    @Override
    public DnsResult processItem(VisitRequest visitRequest) {
        // TODO: resolve A/AAAA/MX/TXT records for visitRequest.getDomainName()
        return DnsResult.builder()
                .domainName(visitRequest.getDomainName())
                .visitId(visitRequest.getVisitId())
                .numRecords(0)
                .hasMx(false)
                .build();
    }
}

