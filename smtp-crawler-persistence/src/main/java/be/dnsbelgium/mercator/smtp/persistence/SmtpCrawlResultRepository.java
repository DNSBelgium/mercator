package be.dnsbelgium.mercator.smtp.persistence;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface SmtpCrawlResultRepository extends PagingAndSortingRepository<SmtpCrawlResult, Long> {

    @Query("select r from SmtpCrawlResult r where r.visitId = :visitId")
    Optional<SmtpCrawlResult> findByVisitId(@Param("visitId") UUID visitId);

}
