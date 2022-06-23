package be.dnsbelgium.mercator.tls.crawler.persistence.repositories;

import be.dnsbelgium.mercator.tls.crawler.persistence.entities.Certificate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CertificateRepository extends PagingAndSortingRepository<Certificate, Long> {

  @Query("select r from Certificate r where r.sha256fingerprint = :fingerprint")
  Optional<Certificate> findBySha256fingerprint(@Param("fingerprint") String fingerprint);

}
