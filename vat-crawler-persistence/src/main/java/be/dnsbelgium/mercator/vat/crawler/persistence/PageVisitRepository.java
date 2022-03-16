package be.dnsbelgium.mercator.vat.crawler.persistence;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PageVisitRepository extends PagingAndSortingRepository<PageVisit, Long>  {

  // Do NOT remove. This is being used in an (not yet committed) test
  @Query(nativeQuery = true, value =
      "select * from vat_crawler.page_visit " +
          " where jsonb_array_length(vat_values) > 0 " +
          " order by id " +
          " limit 20000")
  List<PageVisit> findPageVisitByVatValuesIsNotNull();

  @Query("select r from PageVisit r where r.visitId = :visitId and r.url = :url")
  Optional<PageVisit> findByVisitIdAndUrl(@Param("visitId") UUID visitId, @Param("url") String url);

}
