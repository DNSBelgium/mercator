package be.dnsbelgium.mercator.tls.crawler.persistence.repositories;

import be.dnsbelgium.mercator.tls.crawler.persistence.entities.FullScanEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface FullScanRepository extends PagingAndSortingRepository<FullScanEntity, Long> {

  @Query("select f from FullScanEntity f" +
    "    join CrawlResultEntity cr on f = cr.fullScanEntity" +
    "    where cr.visitId = :visitId")
  List<FullScanEntity> findRelatedToCrawlResult(@Param("visitId") UUID visitId);
}
