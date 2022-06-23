package be.dnsbelgium.mercator.tls.crawler.persistence.repositories;

import be.dnsbelgium.mercator.tls.crawler.persistence.entities.ScanResult;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ScanResultRepository extends PagingAndSortingRepository<ScanResult, Long> {
}
