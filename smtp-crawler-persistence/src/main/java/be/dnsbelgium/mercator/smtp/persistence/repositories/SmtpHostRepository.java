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
    "join smtp_crawler.smtp_server_host sh on sh.host_id = h.id " +
    "join smtp_crawler.smtp_server s on sh.server_id = s.id " +
    "join smtp_crawler.smtp_crawl_result c on c.id = s.crawl_result " +
    "where h.ip = ?1 and (cast(?2 as timestamp) - c.crawl_timestamp) < '24 hours' ",
    nativeQuery = true)
  Optional<SmtpHostEntity> findRecentCrawlByIp(@Param("ip") String ip, @Param("date_time") ZonedDateTime dateTime);
}