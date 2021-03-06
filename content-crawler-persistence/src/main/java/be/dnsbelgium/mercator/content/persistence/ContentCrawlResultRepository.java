package be.dnsbelgium.mercator.content.persistence;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ContentCrawlResultRepository extends PagingAndSortingRepository<ContentCrawlResult, Long> {
  long countByVisitId(UUID visitId);

  //used by frontend through automatic exposure in the API
  List<ContentCrawlResult> findByVisitId(@Param("visitId") UUID visitId);

  /**
   * save given ContentCrawlResult and ignore violations of unique constraint on (visit_id, url)
   *
   * @param crawlResult the entity to save
   * @return true if save failed because of unique constraint on (visit_id, url)
   * throws DataIntegrityViolationException for other integrity constraint violations
   */
  default boolean saveAndIgnoreDuplicateKeys(ContentCrawlResult crawlResult) {
    try {
      save(crawlResult);
      return false;
    } catch (DataIntegrityViolationException e) {
      if (e.getMessage() != null && e.getMessage().contains("content_crawler_visitid_url_uq")) {
        // error is already logged by SqlExceptionHelper
        return true;
      } else {
        throw e;
      }
    }
  }

  List<ContentCrawlResult> findByVisitIdAndOk(UUID visitId, boolean ok);

  default List<ContentCrawlResult> findSucceededCrawlsByVisitId(UUID visitId) {
    return findByVisitIdAndOk(visitId, true);
  }

}