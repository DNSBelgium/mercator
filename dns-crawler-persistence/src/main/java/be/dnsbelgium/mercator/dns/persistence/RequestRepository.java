package be.dnsbelgium.mercator.dns.persistence;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RequestRepository extends PagingAndSortingRepository<Request, Long> {

    @Query("select r from Request r where r.visitId = :visitId")
    Optional<Request> findByVisitId(@Param("visitId") UUID visitId);

}
