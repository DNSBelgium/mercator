package be.dnsbelgium.mercator.smtp.persistence.repositories;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SmtpConversationRepository extends PagingAndSortingRepository<SmtpConversation, Long> {

  @Query(value = "select * from smtp_conversation " +
    "inner join smtp_host sh on smtp_conversation.id = sh.conversation " +
    "where sh.visit_id = :visit_id", nativeQuery = true)
  List<SmtpConversation> findAllByVisitId(@Param("visit_id") UUID visitId);

  @SuppressWarnings("unused") // method is used by the UI (via the REST API)
  @Query(value = "select * from smtp_conversation c " +
      "inner join smtp_host h on c.id = h.conversation " +
      "where h.id = ?1", nativeQuery = true)
  SmtpConversation findByHostId(@Param("host_id") Long hostId);

}