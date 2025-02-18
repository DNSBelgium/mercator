package be.dnsbelgium.mercator.tls.crawler.persistence.repositories;

import be.dnsbelgium.mercator.tls.domain.BlacklistEntry;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BlacklistEntryRepository  {

  public List<BlacklistEntry> findAll() {
    return List.of();
  }

}
