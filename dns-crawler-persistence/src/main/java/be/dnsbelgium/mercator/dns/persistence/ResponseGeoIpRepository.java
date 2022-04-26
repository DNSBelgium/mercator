package be.dnsbelgium.mercator.dns.persistence;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;
import java.util.UUID;

public interface ResponseGeoIpRepository extends PagingAndSortingRepository<ResponseGeoIp, Long> {

    List<ResponseGeoIp> findAllByResponseId(long responseId);

    List<ResponseGeoIp> findAllByResponseRequestVisitId(UUID visitId);
}
