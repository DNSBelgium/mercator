package be.dnsbelgium.mercator.tls.crawler.persistence.repositories;

import be.dnsbelgium.mercator.tls.crawler.persistence.entities.CrawlResultEntity;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface CrawlResultRepository extends PagingAndSortingRepository<CrawlResultEntity, Long> {
  List<CrawlResultEntity> findByVisitId(@Param("visitId") UUID visitId);
}
