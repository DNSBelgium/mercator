package be.dnsbelgium.mercator.dns.persistence;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.Optional;
import java.util.UUID;

public interface RequestRepository extends PagingAndSortingRepository<Request, Long> {

    Optional<Request> findRequestByVisitId(UUID visitId);

}
