package be.dnsbelgium.mercator.ssl.crawler.persistence.repositories;

import be.dnsbelgium.mercator.ssl.crawler.persistence.entities.CountCipherSuitesResult;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SupportCipherSuiteCountRepository extends PagingAndSortingRepository<CountCipherSuitesResult, String> {

  @Query(value = "select css.protocol as protocol, count(*) as count" +
      "  from cipher_suite cs" +
      "  join cipher_suite_support css on cs.iana_name = css.cipher_suite_id" +
      "  join ssl_crawl_result scr on css.ssl_crawl_result_id = scr.id" +
      "  where scr.visit_id = CAST(?1 AS uuid)" +
      "  and css.supported" +
      "  group by css.protocol",
      nativeQuery = true)
  List<CountCipherSuitesResult> findNumberOfSupportedCipherSuitesByVisitId(@Param("visitId") String visitId);
}
