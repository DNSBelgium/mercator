package be.dnsbelgium.mercator.smtp.persistence.repositories;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHostEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SmtpHostRepository extends PagingAndSortingRepository<SmtpHostEntity, Long> {
  @Query(value = "select * from smtp_host where visit_id = :visit_id", nativeQuery = true)
  List<SmtpHostEntity> findAllByVisitId(@Param("visit_id") UUID visitId);
}
