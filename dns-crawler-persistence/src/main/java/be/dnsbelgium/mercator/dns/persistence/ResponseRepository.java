package be.dnsbelgium.mercator.dns.persistence;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.UUID;

public interface ResponseRepository extends PagingAndSortingRepository<Response, Long> {

    List<Response> findAllByRequestId(long requestId);

    List<Response> findAllByRequestVisitId(UUID visitId);

}
