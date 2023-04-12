package be.dnsbelgium.mercator.smtp.persistence.repositories;

import be.dnsbelgium.mercator.smtp.persistence.entities.ExtensionEntity;
import org.springframework.data.repository.PagingAndSortingRepository;

public interface ExtensionRepository extends PagingAndSortingRepository<ExtensionEntity, Long> {
}
