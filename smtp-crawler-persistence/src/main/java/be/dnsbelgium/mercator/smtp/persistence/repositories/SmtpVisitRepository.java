package be.dnsbelgium.mercator.smtp.persistence.repositories;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisitEntity;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;
import java.util.UUID;

public interface SmtpVisitRepository extends PagingAndSortingRepository<SmtpVisitEntity, Long> {
  Optional<SmtpVisitEntity> findByVisitId(UUID visitId);
}
