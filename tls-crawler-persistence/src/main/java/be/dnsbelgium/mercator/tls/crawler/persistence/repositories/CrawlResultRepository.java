package be.dnsbelgium.mercator.tls.crawler.persistence.repositories;

import be.dnsbelgium.mercator.tls.crawler.persistence.entities.CrawlResultEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CrawlResultRepository extends PagingAndSortingRepository<CrawlResultEntity, Long> {

  @Query("select r from CrawlResultEntity r where r.visitId = :visitId")
  Optional<CrawlResultEntity> findByVisitId(@Param("visitId")UUID visitId);
}
