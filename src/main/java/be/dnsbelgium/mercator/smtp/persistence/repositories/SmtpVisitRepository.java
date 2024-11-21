package be.dnsbelgium.mercator.smtp.persistence.repositories;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SmtpVisitRepository  {

  private static final Logger logger = LoggerFactory.getLogger(SmtpVisitRepository.class);

  public Optional<SmtpVisit> findByVisitId(String visitId) {
    logger.info("visitId = {}", visitId);
    return Optional.empty();
  }

  public SmtpVisit save(SmtpVisit visit) {
      return visit;
  }
}
