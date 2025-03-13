package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.TlsRepository;
import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = "be.dnsbelgium.mercator.mvc")
public class TlsSearchControllerTest {

    @MockitoBean
    private TlsRepository tlsRepository;

    private final ObjectMother objectMother = new ObjectMother();

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void searchVisitIds_found() throws Exception {
        when(tlsRepository.searchVisitIds("dnsbelgium.be")).thenReturn(List.of("v101", "v102", "v103"));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/tls/ids")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("search-results-tls"))
                .andExpect(model().attributeExists( "visitIds"))
                .andExpect(content().string(containsString("v101")))
        ;

    }

    @Test
    public void searchVisitIds_notFound() throws Exception {
        when(tlsRepository.searchVisitIds("dnsbelgium.be")).thenReturn(List.of());
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/tls/ids")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("search-results-tls"))
                .andExpect(model().attributeExists( "visitIds"))
                .andExpect(content().string(containsString("No TLS visits found for")))
        ;
    }

    @Test
    public void findByVisitId_found() throws Exception {
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
    public void findByVisitId_notFound() throws Exception {
        when(tlsRepository.findByVisitId("idjsfijoze-er-ze")).thenReturn(Optional.empty());
        this.mockMvc
                .perform(MockMvcRequestBuilders
                        .get("/search/tls/id")
                        .param("visitId", "idjsfijoze-er-ze")
                ).andExpect(content().string(containsString("No TLS crawl results found for visit-id <strong>idjsfijoze-er-ze</strong>")));
    }

    @Test
    public void findLatestResult_found() throws Exception {
        TlsCrawlResult tlsCrawlResult1  = objectMother.tlsCrawlResult1();
        when(tlsRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.ofNullable(tlsCrawlResult1));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/tls/latest")
                        .param("domainName", "dnsbelgium.be")
                        .param("fetchLatest", "true"))
                .andExpect(view().name("visit-details-tls"))
                .andExpect(model().attributeExists( "domainName"))
                .andExpect(content().string(containsString("aakjkjkj-ojj")));

    }
}
