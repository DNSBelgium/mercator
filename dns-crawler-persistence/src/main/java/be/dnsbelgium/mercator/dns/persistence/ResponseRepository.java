package be.dnsbelgium.mercator.dns.persistence;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.PagingAndSortingRepository;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface ResponseRepository extends PagingAndSortingRepository<Response, Long> {

    List<Response> findAllByRequestId(long requestId);

//    @Query("SELECT r                        \n" +
//            "FROM Response r                \n" +
//            "WHERE r.requestId = (          \n" +
//            "    SELECT req.id                  \n" +
//            "    FROM Request req   \n" +
//            "    WHERE req.visitId = ':visitId')")
//    List<Response> findAllByVisitId(@Param("visitId") UUID visitId);

    List<Response> findAllByRequestVisitId(UUID visitId);

}
