package be.dnsbelgium.mercator.persistence.repository;

import be.dnsbelgium.mercator.persistence.SearchRepository;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.Optional;

class SearchRepositoryTest {

    SearchRepository searchRepository = new SearchRepository();

    @Test
    @Disabled
    public void findTlsCrawlResult() throws JsonProcessingException {
        // TODO: this test depends on state that is not managed by the test: 'tls.parquet'
        TlsCrawlResult tls = searchRepository.findTlsCrawlResult("d1894946-e41d-42f7-9096-939a18bbc3dd");
        System.out.println("Tls: " + tls);


    }

    @Test
    @Disabled
    public void findSmtpConversationResult() throws JsonProcessingException {
        // TODO: prepare data, do not depend on output of other tests
        SmtpConversation smtpConversation = searchRepository.findSmtpConversationResult("d1894946-e41d-42f7-9096-939a18bbc3dd");
        System.out.println("Smtp: " + smtpConversation);
        // what do you want to test ? Give method a proper name
        // TODO: add asserts
    }

    @Test
    @Disabled
    public void findLatestTlsResult() throws JsonProcessingException {
        // TODO: prepare data, do not depend on output of other tests
        Optional<String> result = searchRepository.searchlatestTlsResult();
        System.out.println(result);
        // TODO: add asserts
        // TODO: no System.out
    }

}
