package be.dnsbelgium.mercator.smtp.persistence.repositories;

import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpHostEntity;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface SmtpHostRepository extends PagingAndSortingRepository<SmtpHostEntity, Long> {
}
