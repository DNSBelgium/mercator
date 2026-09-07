package be.dnsbelgium.mercator.pipeline.smtp;

import lombok.Builder;
import lombok.Getter;

/** Placeholder SMTP crawl result. Extend with real data (banner, TLS, MX hosts, …) later. */
@Builder
@Getter
public class SmtpResult {

    private String domainName;
    private String visitId;
    private String banner;
    private boolean supportsStartTls;
}

