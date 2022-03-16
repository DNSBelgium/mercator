package be.dnsbelgium.mercator.ssl.crawler.persistence.repositories;

import be.dnsbelgium.mercator.ssl.crawler.persistence.entities.SslCrawlResult;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SslCrawlResultRepository extends PagingAndSortingRepository<SslCrawlResult, Long> {

  @Query(value = "select * from ssl_crawl_result r where r.visit_id = CAST(?1 AS uuid) limit 1", nativeQuery = true)
  Optional<SslCrawlResult> findOneByVisitId(@Param("visitId")UUID visitId);
}
