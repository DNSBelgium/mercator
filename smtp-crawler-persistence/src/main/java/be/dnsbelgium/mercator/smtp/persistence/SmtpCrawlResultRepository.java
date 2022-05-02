package be.dnsbelgium.mercator.smtp.persistence;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SmtpCrawlResultRepository extends PagingAndSortingRepository<SmtpCrawlResult, Long> {

    Optional<SmtpCrawlResult> findFirstByVisitId(@Param("visitId") UUID visitId); // TODO: Fix duplicate visitIds.

}
