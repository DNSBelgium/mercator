package be.dnsbelgium.mercator.smtp.persistence.repositories;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisitEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

public interface SmtpVisitRepository extends PagingAndSortingRepository<SmtpVisitEntity, Long> {
  Optional<SmtpVisitEntity> findByVisitId(UUID visitId);

  default Optional<SmtpVisitEntity> saveAndIgnoreDuplicateKeys(SmtpVisitEntity smtpVisitEntity) {
    try {
      SmtpVisitEntity visitEntity = save(smtpVisitEntity);
      return Optional.of(visitEntity);
    } catch (DataIntegrityViolationException e) {
      //TODO test if "smtp_visit_pkey_uq" is the correct string
      if (e.getMessage() != null && e.getMessage().contains("smtp_visit_pkey_uq")) {
        // error is already logged by SqlExceptionHelper
        getLogger(SmtpVisitRepository.class).info("Acceptable DataIntegrityViolationException: {}", e.getMessage());
        return Optional.empty();
      } else {
        getLogger(SmtpVisitRepository.class).info("Other DataIntegrityViolationException: {}", e.getMessage());
        throw e;
      }
    }
  }
}
