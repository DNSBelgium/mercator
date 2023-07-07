package be.dnsbelgium.mercator.smtp.persistence.repositories;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversation;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SmtpConversationRepository extends PagingAndSortingRepository<SmtpConversation, Long> {

  @SuppressWarnings("unused") // method is used by the UI (via the REST API) see mercator-ui/src/components/detailsCards/SMTPCard.jsx
  @Query(value = "select * from smtp_conversation c " +
      "inner join smtp_host h on c.id = h.conversation " +
      "where h.id = ?1", nativeQuery = true)
  SmtpConversation findByHostId(@Param("host_id") Long hostId);

}