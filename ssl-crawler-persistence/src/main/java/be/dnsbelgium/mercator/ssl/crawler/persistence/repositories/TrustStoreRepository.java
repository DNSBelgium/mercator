package be.dnsbelgium.mercator.ssl.crawler.persistence.repositories;

import be.dnsbelgium.mercator.ssl.crawler.persistence.entities.TrustStore;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TrustStoreRepository extends PagingAndSortingRepository<TrustStore, Long> {

  @Query("select r from TrustStore r where r.id = :id")
  Optional<TrustStore> findById(@Param("id") Long id);

  @Query(value = "select * from trust_store ts join check_against_trust_store c on c.trust_store_id = ts.id where c.certificate_deployment_id = ?1",
      nativeQuery = true)
  List<TrustStore> findRelatedToCertificateDeployment(@Param("certDeployId") Long certDeployId);

  @Query(value = "select * from trust_store ts" +
      "      join check_against_trust_store cats on ts.id = cats.trust_store_id" +
      "      join certificate_deployment cd on cd.id = cats.certificate_deployment_id" +
      "      join ssl_crawl_result scr on scr.id = cd.ssl_crawl_result_id" +
      "      where scr.visit_id = CAST(?1 AS uuid)",
      nativeQuery = true)
  List<TrustStore> findRelatedToSslCrawlResult(@Param("visitId") String visitId);
}
