package be.dnsbelgium.mercator.smtp.persistence.repositories;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpServerEntity;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface SmtpServerRepository extends PagingAndSortingRepository<SmtpServerEntity, Long> {
}
