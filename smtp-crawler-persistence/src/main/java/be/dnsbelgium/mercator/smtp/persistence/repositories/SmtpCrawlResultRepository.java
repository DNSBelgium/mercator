/*TODO remove
package be.dnsbelgium.mercator.smtp.persistence.repositories;


import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpCrawlResult;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

public interface SmtpCrawlResultRepository extends PagingAndSortingRepository<SmtpCrawlResult, Long> {


  Optional<SmtpCrawlResult> findFirstByVisitId(@Param("visitId") UUID visitId); // TODO: Fix duplicate visitIds.

  List<SmtpCrawlResult> findByVisitId(@Param("visitId") UUID visitId);

  /**
   * save given ContentCrawlResult and ignore violations of unique constraint on (visit_id, url)
   *
   * @param smtpCrawlResult the entity to save
   * @return true if save failed because of unique constraint
   * throws DataIntegrityViolationException for other integrity constraint violations
   */
/*
  default boolean saveAndIgnoreDuplicateKeys(SmtpCrawlResult smtpCrawlResult) {
    try {
      save(smtpCrawlResult);
      return false;
    } catch (DataIntegrityViolationException e) {
      if (e.getMessage() != null && e.getMessage().contains("smtp_crawl_result_visitid_uq")) {
        // error is already logged by SqlExceptionHelper
        getLogger(SmtpCrawlResultRepository.class).info("Acceptable DataIntegrityViolationException: {}", e.getMessage());
        return true;
      } else {
        getLogger(SmtpCrawlResultRepository.class).info("Other DataIntegrityViolationException: {}", e.getMessage());
        throw e;
      }
    }
  }
}

 */
