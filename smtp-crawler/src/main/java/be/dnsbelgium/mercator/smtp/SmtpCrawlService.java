package be.dnsbelgium.mercator.smtp;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.smtp.domain.crawler.SmtpAnalyzer;
import be.dnsbelgium.mercator.smtp.domain.crawler.SmtpVisit;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.smtp.metrics.MetricName;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversationEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHostEntity;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisitEntity;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpConversationRepository;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpVisitRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class SmtpCrawlService {

  private static final Logger logger = getLogger(SmtpCrawlService.class);

  private final SmtpVisitRepository repository;
  private final SmtpConversationRepository conversationRepository;
  private final SmtpAnalyzer analyzer;
  private final MeterRegistry meterRegistry;

  public SmtpCrawlService(SmtpVisitRepository repository, SmtpConversationRepository conversationRepository, SmtpAnalyzer analyzer, MeterRegistry meterRegistry) {
    this.repository = repository;
    this.conversationRepository = conversationRepository;
    this.analyzer = analyzer;
    this.meterRegistry = meterRegistry;
  }


  public SmtpVisit retrieveSmtpInfo(VisitRequest visitRequest) throws Exception {
    String fqdn = visitRequest.getDomainName();
    logger.debug("Retrieving SMTP info for domainName = {}", fqdn);
    SmtpVisit result = analyzer.analyze(fqdn);
    result.setVisitId(visitRequest.getVisitId());
    return result;
  }

  @Transactional
  public Optional<SmtpVisitEntity> find(UUID visitId) {
    Optional<SmtpVisitEntity> smtpVisit = repository.findByVisitId(visitId);
    logger.debug("find by visitId => {}", smtpVisit);
    return smtpVisit;
  }

  @Transactional
  public List<SmtpConversation> save(SmtpVisit smtpVisit) {
    logger.debug("About to save SmtpVisitEntity for {}", smtpVisit.getDomainName());
    SmtpVisitEntity visitEntity = smtpVisit.toEntity();
    List<SmtpConversation> newSavedConversations = new ArrayList<>();
    for (SmtpHostEntity host : visitEntity.getHosts()) {
      if (host.getConversation().getId() != null){
        Optional<SmtpConversationEntity> conversationEntity = conversationRepository.findById(host.getConversation().getId());
        conversationEntity.ifPresent(host::setConversation);
      }
      else {
        newSavedConversations.add(conversationRepository.save(host.getConversation()).toSmtpConversation());
      }
    }
    Optional<SmtpVisitEntity> savedVisit = repository.saveAndIgnoreDuplicateKeys(visitEntity);
    if (savedVisit.isEmpty()) {
      logger.info("was duplicate: {}", smtpVisit.getVisitId());
      meterRegistry.counter(MetricName.COUNTER_DUPLICATE_KEYS).increment();
      // note that transaction will be rolled back by Hibernate
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
      logger.debug("Saved SmtpVisitEntity for {} with visitId={}",
        smtpVisit.getDomainName(), smtpVisit.getVisitId());
    }
    return newSavedConversations;
  }

}
