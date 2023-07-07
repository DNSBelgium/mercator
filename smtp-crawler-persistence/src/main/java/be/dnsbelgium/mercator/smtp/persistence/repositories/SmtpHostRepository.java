package be.dnsbelgium.mercator.smtp.persistence.repositories;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpConversation;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHost;
import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.UUID;

public interface SmtpHostRepository extends PagingAndSortingRepository<SmtpHost, Long> {

  // Do not remove. Method is used by the UI. See mercator-ui/src/components/detailsCards/SMTPCard.jsx
  List<SmtpHost> findByVisitVisitId(UUID visitId);

}
