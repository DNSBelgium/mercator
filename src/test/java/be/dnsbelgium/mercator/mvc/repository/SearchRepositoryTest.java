package be.dnsbelgium.mercator.mvc.repository;

import be.dnsbelgium.mercator.persistence.SearchRepository;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

class SearchRepositoryTest {

    SearchRepository searchRepository = new SearchRepository();

    @Test
    public void findTlsCrawlResult() throws JsonProcessingException {
        TlsCrawlResult tls = searchRepository.findTlsCrawlResult("d1894946-e41d-42f7-9096-939a18bbc3dd");
        System.out.println("Tls: " + tls);


    }

}
