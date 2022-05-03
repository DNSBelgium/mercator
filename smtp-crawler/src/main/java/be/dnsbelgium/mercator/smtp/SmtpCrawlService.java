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
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
        Optional<SmtpCrawlResult> crawlResult = repository.findFirstByVisitId(visitId);
        logger.debug("find by visitId => {}", crawlResult);
        return crawlResult;
    }

    @Transactional
    public void save(SmtpCrawlResult smtpCrawlResult) {
        logger.debug("About to save SmtpCrawlResult for {}", smtpCrawlResult.getDomainName());
        boolean wasDuplicate = repository.saveAndIgnoreDuplicateKeys(smtpCrawlResult);
        if (wasDuplicate) {
            logger.info("was duplicate: {}", smtpCrawlResult.getVisitId());
            meterRegistry.counter(MetricName.COUNTER_DUPLICATE_KEYS).increment();
            // note that transaction will be rolled back by Hibernate
            logger.info("TransactionSynchronizationManager.isActualTransactionActive() = {}", TransactionSynchronizationManager.isActualTransactionActive());
            // even though we return here without an exception,
            // spring will throw an UnexpectedRollbackException ("Transaction silently rolled back because it has been marked as rollback-only")
            // to the calling code, probably because of the @Transactional
            // so the distinction made in saveAndIgnoreDuplicateKeys between smtp_crawl_result_visitid_uq and other exceptions
            // makes no difference: the tx is rolled back and caller if this method gets an exception
            // Next JMS tries to interfere and calls changeMessageVisibilityBatch which leads to
            // com.amazonaws.SdkClientException: Unable to unmarshall response (ParseError at [row,col]:[1,157]
            // Message: The processing instruction target matching "[xX][mM][lL]" is not allowed.). Response Code: 200, Response Text:
            // Could be related to the use of localstack.
        } else {
            meterRegistry.counter(MetricName.SMTP_RESULTS_SAVED).increment();
            logger.debug("Saved SmtpCrawlResult for {} with visitId={}",
                smtpCrawlResult.getDomainName(), smtpCrawlResult.getVisitId());
        }
    }

}
