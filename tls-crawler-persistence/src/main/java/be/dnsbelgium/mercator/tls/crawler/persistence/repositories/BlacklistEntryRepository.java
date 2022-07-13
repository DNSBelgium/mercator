package be.dnsbelgium.mercator.tls.crawler.persistence.repositories;

import be.dnsbelgium.mercator.tls.crawler.persistence.entities.BlacklistEntry;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface BlacklistEntryRepository extends PagingAndSortingRepository<BlacklistEntry, String> {

  List<BlacklistEntry> findAll();
}
