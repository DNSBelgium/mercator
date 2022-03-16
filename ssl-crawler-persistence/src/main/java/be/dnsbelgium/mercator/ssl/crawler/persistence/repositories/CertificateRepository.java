package be.dnsbelgium.mercator.ssl.crawler.persistence.repositories;

import be.dnsbelgium.mercator.ssl.crawler.persistence.entities.Certificate;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CertificateRepository extends PagingAndSortingRepository<Certificate, Long> {

  @Query("select r from Certificate r where r.sha256fingerprint = :fingerprint")
  Optional<Certificate> findBySha256fingerprint(@Param("fingerprint") String fingerprint);

  @Query(value = "select * from certificate c join certificate_deployment cd on c.sha256_fingerprint = cd.leaf_certificate_sha256 where cd.id = ?1",
      nativeQuery = true)
  List<Certificate> findRelatedToCertificateDeployment(@Param("certDeployId") Long certDeployId);

  @Query(value = "select * from certificate c" +
      "    join certificate_deployment cd on c.sha256_fingerprint = cd.leaf_certificate_sha256" +
      "    join ssl_crawl_result scr on cd.ssl_crawl_result_id = scr.id" +
      "    where scr.visit_id = CAST(?1 AS uuid)",
      nativeQuery = true)
  List<Certificate> findRelatedToSslCrawlResult(@Param("visitId") String visitId);
}
