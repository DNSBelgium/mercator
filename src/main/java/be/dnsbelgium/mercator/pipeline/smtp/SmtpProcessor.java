package be.dnsbelgium.mercator.pipeline.smtp;

import be.dnsbelgium.mercator.pipeline.service.ItemProcessor;
import be.dnsbelgium.mercator.pipeline.service.VisitRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/** Placeholder SMTP processor. TODO: connect to MX hosts and probe SMTP/STARTTLS. */
@Slf4j
@Component
public class SmtpProcessor implements ItemProcessor<VisitRequest, SmtpResult> {

    @Override
    public SmtpResult processItem(VisitRequest visitRequest) {
        // TODO: look up MX hosts and open an SMTP connection for visitRequest.getDomainName()
        return SmtpResult.builder()
                .domainName(visitRequest.getDomainName())
                .visitId(visitRequest.getVisitId())
                .banner(null)
                .supportsStartTls(false)
                .build();
    }
}

