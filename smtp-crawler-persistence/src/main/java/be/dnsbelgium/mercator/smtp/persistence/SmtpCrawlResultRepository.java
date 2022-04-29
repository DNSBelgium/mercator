package be.dnsbelgium.mercator.smtp.persistence;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;
import java.util.UUID;

public interface SmtpCrawlResultRepository extends PagingAndSortingRepository<SmtpCrawlResult, Long> {

    Optional<SmtpCrawlResult> findFirstByVisitId(UUID visitId); // TODO: Fix duplicate visitIds.

}
