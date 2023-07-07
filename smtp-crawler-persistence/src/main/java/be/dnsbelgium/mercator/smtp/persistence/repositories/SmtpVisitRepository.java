package be.dnsbelgium.mercator.smtp.persistence.repositories;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisit;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;


public interface SmtpVisitRepository extends PagingAndSortingRepository<SmtpVisit, Long> {
  Optional<SmtpVisit> findByVisitId(@Param("visitId") UUID visitId);
}
