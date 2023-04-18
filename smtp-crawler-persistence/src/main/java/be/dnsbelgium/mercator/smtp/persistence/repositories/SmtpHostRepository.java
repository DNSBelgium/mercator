package be.dnsbelgium.mercator.smtp.persistence.repositories;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHostEntity;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.net.InetAddress;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

public interface SmtpHostRepository extends PagingAndSortingRepository<SmtpHostEntity, Long> {
  @Query(value = "select * from smtp_crawler.smtp_host h " +
    "where h.ip = ?1 and (cast(?2 as timestamp) - h.timestamp) < '24 hours' " +
    "order by h.timestamp desc " +
    "limit 1",
    nativeQuery = true)
  Optional<SmtpHostEntity> findRecentCrawlByIp(@Param("ip") String ip, @Param("date_time") ZonedDateTime dateTime);
}