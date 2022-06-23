package be.dnsbelgium.mercator.tls.crawler.persistence.repositories;

import be.dnsbelgium.mercator.tls.crawler.persistence.entities.TlsScanResult;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface TlsScanResultRepository extends PagingAndSortingRepository<TlsScanResult, Long> {


}
