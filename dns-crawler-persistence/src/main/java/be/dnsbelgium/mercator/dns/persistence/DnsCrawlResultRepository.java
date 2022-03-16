package be.dnsbelgium.mercator.dns.persistence;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface DnsCrawlResultRepository extends PagingAndSortingRepository<DnsCrawlResult, Long> {

  @Query("select r from DnsCrawlResult r where r.visitId = :visitId")
  Optional<DnsCrawlResult> findByVisitId(@Param("visitId") UUID visitId);

}
