package be.dnsbelgium.mercator.smtp.persistence.repositories;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversationEntity;
import com.vladmihalcea.hibernate.type.interval.PostgreSQLIntervalType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SmtpConversationRepository extends PagingAndSortingRepository<SmtpConversationEntity, Long> {
  @Query(value = "select * from smtp_crawler.smtp_conversation h " +
    "where h.ip = ?1 and  h.timestamp > ?2  " +
    "order by h.timestamp desc " +
    "limit 1",
    nativeQuery = true)
  Optional<SmtpConversationEntity> findRecentCrawlByIp(@Param("ip") String ip, @Param("date_time") ZonedDateTime dateTime);

  Optional<SmtpConversationEntity> findByIpAndTimestamp(String ip, ZonedDateTime timestamp);

  @Query(value = "select * from smtp_crawler.smtp_conversation " +
    "inner join smtp_crawler.smtp_host sh on smtp_conversation.id = sh.conversation " +
    "where sh.visit_id = :visit_id", nativeQuery = true)
  List<SmtpConversationEntity> findAllByVisitId(@Param("visit_id") UUID visitId);

  @Query(value = "select * from smtp_crawler.smtp_conversation c " +
    "inner join smtp_crawler.smtp_host h on c.id = h.conversation " +
    "where h.id = ?1", nativeQuery = true)
  SmtpConversationEntity findByHostId(@Param("host_id") Long hostId);
}