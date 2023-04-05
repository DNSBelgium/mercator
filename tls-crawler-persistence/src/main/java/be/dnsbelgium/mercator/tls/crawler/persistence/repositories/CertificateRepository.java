package be.dnsbelgium.mercator.tls.crawler.persistence.repositories;

import be.dnsbelgium.mercator.tls.crawler.persistence.entities.CertificateEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CertificateRepository extends PagingAndSortingRepository<CertificateEntity, Long> {

  @Query("select c from CertificateEntity c where c.sha256fingerprint = :fingerprint")
  Optional<CertificateEntity> findBySha256fingerprint(@Param("fingerprint") String fingerprint);

  @Query("select c from CertificateEntity c" +
    "    join CrawlResultEntity cr on c.sha256fingerprint = cr.leafCertificateEntity" +
    "    where cr.visitId = :visitId")
  List<CertificateEntity> findRelatedToCrawlResult(@Param("visitId") UUID visitId);

}
