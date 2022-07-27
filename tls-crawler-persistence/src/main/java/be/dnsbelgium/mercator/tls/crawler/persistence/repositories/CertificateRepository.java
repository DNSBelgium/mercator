package be.dnsbelgium.mercator.tls.crawler.persistence.repositories;

import be.dnsbelgium.mercator.tls.crawler.persistence.entities.CertificateEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CertificateRepository extends PagingAndSortingRepository<CertificateEntity, Long> {

  @Query("select c from CertificateEntity c where c.sha256fingerprint = :fingerprint")
  Optional<CertificateEntity> findBySha256fingerprint(@Param("fingerprint") String fingerprint);

}
