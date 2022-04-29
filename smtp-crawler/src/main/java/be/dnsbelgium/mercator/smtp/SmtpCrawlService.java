package be.dnsbelgium.mercator.smtp;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.smtp.domain.crawler.SmtpAnalyzer;
import be.dnsbelgium.mercator.smtp.metrics.MetricName;
import be.dnsbelgium.mercator.smtp.persistence.SmtpCrawlResult;
import be.dnsbelgium.mercator.smtp.persistence.SmtpCrawlResultRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

import java.util.Optional;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class SmtpCrawlService {

    private static final Logger logger = getLogger(SmtpCrawlService.class);

    private final SmtpCrawlResultRepository repository;
    private final SmtpAnalyzer analyzer;
    private final MeterRegistry meterRegistry;

    public SmtpCrawlService(SmtpCrawlResultRepository repository, SmtpAnalyzer analyzer, MeterRegistry meterRegistry) {
        this.repository = repository;
        this.analyzer = analyzer;
        this.meterRegistry = meterRegistry;
    }

    public SmtpCrawlResult retrieveSmtpInfo(VisitRequest visitRequest) throws Exception {
        String fqdn = visitRequest.getDomainName();
        logger.debug("Retrieving SMTP info for domainName = {}", fqdn);
        SmtpCrawlResult result = analyzer.analyze(fqdn);
        result.setVisitId(visitRequest.getVisitId());
        return result;
    }

    @Transactional
    public Optional<SmtpCrawlResult> find(UUID visitId) {
        Optional<SmtpCrawlResult> crawlResult = repository.findByVisitId(visitId);
        logger.debug("find by visitId => {}", crawlResult);
        return crawlResult;
    }

    @Transactional
    public void save(SmtpCrawlResult smtpCrawlResult) {
        logger.debug("About to save SmtpCrawlResult for {}", smtpCrawlResult.getDomainName());
        repository.save(smtpCrawlResult);
        meterRegistry.counter(MetricName.SMTP_RESULTS_SAVED).increment();
        logger.debug("Saved SmtpCrawlResult for {} with visitId={} and id={}",
            smtpCrawlResult.getDomainName(), smtpCrawlResult.getVisitId(), smtpCrawlResult.getId());
    }

}
