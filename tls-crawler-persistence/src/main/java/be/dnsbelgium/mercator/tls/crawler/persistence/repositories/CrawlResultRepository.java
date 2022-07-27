package be.dnsbelgium.mercator.tls.crawler.persistence.repositories;

import be.dnsbelgium.mercator.tls.crawler.persistence.entities.CrawlResultEntity;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface CrawlResultRepository extends PagingAndSortingRepository<CrawlResultEntity, Long> {

}
