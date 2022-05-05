package be.dnsbelgium.mercator.dns.persistence;

import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface RequestRepository extends PagingAndSortingRepository<Request, Long> {

    List<Request> findByVisitId(UUID visitId);

}
