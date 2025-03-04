package be.dnsbelgium.mercator.persistence.repository;

import be.dnsbelgium.mercator.persistence.SearchRepository;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class SearchRepositoryTest {

    SearchRepository searchRepository = new SearchRepository();

    @Test
    public void findTlsCrawlResult() throws JsonProcessingException {
        TlsCrawlResult tls = searchRepository.findTlsCrawlResult("d1894946-e41d-42f7-9096-939a18bbc3dd");
        System.out.println("Tls: " + tls);


    }

    @Test
    public void findSmtpConversationResult() throws JsonProcessingException {
        SmtpConversation smtpConversation = searchRepository.findSmtpConversationResult("d1894946-e41d-42f7-9096-939a18bbc3dd");
        System.out.println("Smtp: " + smtpConversation);
    }

    @Test
    public void findLatestTlsResult() throws JsonProcessingException {
        Optional<String> result = searchRepository.searchlatestTlsResult();
        System.out.println(result);
    }

}
