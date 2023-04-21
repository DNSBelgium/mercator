package be.dnsbelgium.mercator.smtp.persistence.repositories;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversationEntity;
import com.vladmihalcea.hibernate.type.interval.PostgreSQLIntervalType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.Optional;

public interface SmtpConversationRepository extends PagingAndSortingRepository<SmtpConversationEntity, Long> {
  @Query(value = "select * from smtp_crawler.smtp_conversation h " +
    "where h.ip = ?1 and  h.timestamp > ?2  " +
    "order by h.timestamp desc " +
    "limit 1",
    nativeQuery = true)
  Optional<SmtpConversationEntity> findRecentCrawlByIp(@Param("ip") String ip, @Param("date_time") ZonedDateTime dateTime);
}