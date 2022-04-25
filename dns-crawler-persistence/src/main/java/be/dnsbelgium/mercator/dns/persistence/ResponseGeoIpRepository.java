package be.dnsbelgium.mercator.dns.persistence;

import org.springframework.data.repository.PagingAndSortingRepository;

public interface ResponseGeoIpRepository extends PagingAndSortingRepository<ResponseGeoIp, Long> {
}
