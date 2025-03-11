package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.TlsRepository;
import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = "be.dnsbelgium.mercator.mvc")
public class SearchTlsResultTest {

    @MockitoBean
    private TlsRepository tlsRepository;

    private final Logger logger = getLogger(SearchTlsResultTest.class);

    private final ObjectMother objectMother = new ObjectMother();

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getTlsIds_findsIds() throws Exception {
        when(tlsRepository.searchVisitIds("dnsbelgium.be")).thenReturn(List.of("v101", "v102", "v103"));
        // Might need to be modified in the future to contain more data than just Id's
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/tls/ids")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("search-results-tls"))
                .andExpect(model().attributeExists( "visitIds"))
                .andExpect(content().string(containsString("v101")))
        ;

    }

    @Test
    @Disabled // todo Bram: check why this test fails
    public void getTlsIds_doesNotfindIds() throws Exception {
        when(tlsRepository.searchVisitIds("dnsbelgium.be")).thenReturn(List.of());
        // Might need to be modified in the future to contain more data than just Id's
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/tls/ids")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("search-results-tls"))
                .andExpect(model().attributeExists( "visitIds"))
                .andExpect(content().string(containsString("No visitIds found for tls of this domain")))
        ;

    }

    @Test
    public void getTls_FindsVisitDetails() throws Exception {
        // this test assumes that the correct tlsCrawlResult object looks like the one in tlsCrawlresult1
        TlsCrawlResult tlsCrawlResult1  = objectMother.tlsCrawlResult1();
        when(tlsRepository.findByVisitId("aakjkjkj-ojj")).thenReturn(Optional.ofNullable(tlsCrawlResult1));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/tls/id")
                        .param("visitId", "aakjkjkj-ojj"))
                .andExpect(view().name("visit-details-tls"))
                .andExpect(model().attributeExists( "visitId"))
                .andExpect(content().string(containsString("aakjkjkj-ojj")));

    }

    @Test
    public void gettls_doesNotfindVisitDetails() throws Exception {
        WebCrawlResult webCrawlResult1 = objectMother.webCrawlResult1();
        when(tlsRepository.findByVisitId("idjsfijoze-er-ze")).thenReturn(Optional.empty());
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/tls/id")
                        .param("visitId", "idjsfijoze-er-ze"))
                .andExpect(content().string(containsString("No tls crawl results found for visitId")));
    }

    @Test
    public void getTlsLatest_FindsLatestVisitDetails() throws Exception {
        // this test assumes that the correct tlsCrawlResult object looks like the one in tlsCrawlresult1
        TlsCrawlResult tlsCrawlResult1  = objectMother.tlsCrawlResult1();
        when(tlsRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.ofNullable(tlsCrawlResult1));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/tls/latest")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("visit-details-tls"))
                .andExpect(model().attributeExists( "domainName"))
                .andExpect(content().string(containsString("aakjkjkj-ojj")));

    }
}
