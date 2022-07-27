package be.dnsbelgium.mercator.tls.crawler.persistence.repositories;

import be.dnsbelgium.mercator.tls.crawler.persistence.entities.FullScanEntity;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface FullScanRepository extends PagingAndSortingRepository<FullScanEntity, Long> {
}
