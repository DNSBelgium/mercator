package be.dnsbelgium.mercator.content.persistence;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface WappalyzerResultRepository extends PagingAndSortingRepository<WappalyzerResult, UUID> {
  long countByVisitId(UUID visitId);

  Optional<WappalyzerResult> findByVisitId(@Param("visitId") UUID uuid);
}
