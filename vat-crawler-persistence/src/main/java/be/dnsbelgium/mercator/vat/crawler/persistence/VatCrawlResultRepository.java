package be.dnsbelgium.mercator.vat.crawler.persistence;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface VatCrawlResultRepository extends PagingAndSortingRepository<VatCrawlResult, Long> {

  @Query("select r from VatCrawlResult r where r.visitId = :visitId")
  Optional<VatCrawlResult> findByVisitId(@Param("visitId")UUID visitId);
}
