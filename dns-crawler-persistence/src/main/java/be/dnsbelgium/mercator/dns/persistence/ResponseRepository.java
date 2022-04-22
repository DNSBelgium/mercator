package be.dnsbelgium.mercator.dns.persistence;

import org.springframework.data.repository.PagingAndSortingRepository;

import java.util.List;

public interface ResponseRepository extends PagingAndSortingRepository<Response, Long> {

    List<Response> findAllByRequest(Request request); //TODO: AvR check functionality.

}
