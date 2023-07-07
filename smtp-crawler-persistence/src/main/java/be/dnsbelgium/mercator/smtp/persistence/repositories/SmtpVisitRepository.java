package be.dnsbelgium.mercator.smtp.persistence.repositories;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisit;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

public interface SmtpVisitRepository extends PagingAndSortingRepository<SmtpVisit, Long> {
  Optional<SmtpVisit> findByVisitId(@Param("visitId") UUID visitId);

  default Optional<SmtpVisit> saveAndIgnoreDuplicateKeys(SmtpVisit smtpVisit) {
    try {
      SmtpVisit visitEntity = save(smtpVisit);
      return Optional.of(visitEntity);
    } catch (DataIntegrityViolationException e) {
      if (e.getMessage() != null && e.getMessage().contains("smtp_visit_pkey_uq")) {
        // error is already logged by SqlExceptionHelper
        getLogger(SmtpVisitRepository.class).info("Acceptable DataIntegrityViolationException: {}", e.getMessage());
        // note that transaction will be rolled back by Hibernate even though we return here without an exception,
        return Optional.empty();
      } else {
        getLogger(SmtpVisitRepository.class).info("Other DataIntegrityViolationException: {}", e.getMessage());
        throw e;
      }
    }
  }
}
