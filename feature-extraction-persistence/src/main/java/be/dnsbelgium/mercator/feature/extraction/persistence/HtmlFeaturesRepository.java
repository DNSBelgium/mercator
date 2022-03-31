package be.dnsbelgium.mercator.feature.extraction.persistence;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;
import java.util.List;

public interface HtmlFeaturesRepository extends PagingAndSortingRepository<HtmlFeatures, Long> {

  // for debugging purposes only
  @Query(nativeQuery = true, value = "select current_user || '@' || current_database() || '.' || current_schema()")
  String getMetaData();

  Optional<HtmlFeatures> findByVisitIdAndUrl(UUID visitId, String url);

  @Query("select r.id from HtmlFeatures r where r.visitId = :visitId and r.url = :url")
  Optional<Long> selectIdByVisitIdAndUrl(@Param("visitId")UUID visitId, @Param("url")String url);

  List<HtmlFeatures> findByVisitId(@Param("visitId") UUID visitId);

  /**
   * Tries to save the given HtmlFeatures and will ignore duplicate key violation but not other data integrity problems
   * @param htmlFeatures the features to save
   * @return true when html_visit_url_uq constraint was violated
   */
  default boolean saveAndIgnoreDuplicateKeys(HtmlFeatures htmlFeatures) {
    boolean isAcceptableDuplicateKeyViolation = false;
    try {
      save(htmlFeatures);
    } catch (DataIntegrityViolationException dataIntegrityViolationException) {
      Throwable cause = dataIntegrityViolationException.getCause();
      while (cause != null && !isAcceptableDuplicateKeyViolation) {
        isAcceptableDuplicateKeyViolation = (cause.getMessage().contains("html_visit_url_uq"));
        if (cause.getCause() == cause) {
          // avoid endless loops
          break;
        }
        cause = cause.getCause();
      }
      if (!isAcceptableDuplicateKeyViolation) {
        throw dataIntegrityViolationException;
      }
    }
    return isAcceptableDuplicateKeyViolation;
  }

}
