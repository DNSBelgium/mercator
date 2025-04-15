package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.common.VisitIdGenerator;
import be.dnsbelgium.mercator.persistence.SearchVisitIdResultItem;
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
import java.util.stream.Stream;

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
        when(tlsRepository.searchVisitIds("dnsbelgium.be")).thenReturn(Stream.of("v101", "v102", "v103").map(id -> new SearchVisitIdResultItem(id, null)).toList());
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
                .andExpect(content().string(containsString("No TLS crawls found for <span>dnsbelgium.be</span>")))
        ;
    }

    @Test
    public void findByVisitId_found() throws Exception {
        TlsCrawlResult tlsCrawlResult = objectMother.tlsCrawlResult1();

        when(tlsRepository
                .findByVisitId(tlsCrawlResult.getVisitId()))
                .thenReturn(Optional.of(tlsCrawlResult));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/tls/id")
                        .param("visitId", tlsCrawlResult.getVisitId()))
                .andExpect(view().name("visit-details-tls"))
                .andExpect(model().attributeExists( "visitId"))
                .andExpect(content().string(containsString(tlsCrawlResult.getVisitId())));

    }

    @Test
    public void findByVisitId_notFound() throws Exception {
        String visitId = VisitIdGenerator.generate();
        when(tlsRepository.findByVisitId(visitId)).thenReturn(Optional.empty());
        this.mockMvc
                .perform(MockMvcRequestBuilders
                        .get("/search/tls/id")
                        .param("visitId", visitId)
                ).andExpect(content().string(containsString("No TLS crawl results found for visit-id <strong>" + visitId + "</strong>")));
    }

    @Test
    public void findLatestResult_found() throws Exception {
        TlsCrawlResult tlsVisit1 = objectMother.tlsCrawlResult1();
        String domainName = tlsVisit1.getDomainName();
        when(tlsRepository
                .findLatestResult(domainName))
                .thenReturn(Optional.of(tlsVisit1));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/tls/latest")
                        .param("domainName", domainName)
                        .param("fetchLatest", "true"))
                .andExpect(view().name("visit-details-tls"))
                .andExpect(model().attributeExists( "domainName"))
                .andExpect(content().string(containsString(domainName)));

    }

    @Test
    public void findLatest_whenTlsCrawlResultWithNullValues() throws Exception {
        TlsCrawlResult tlsVisit1 = objectMother.tlsCrawlResultWithNullValues();
        when(tlsRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.ofNullable(tlsVisit1));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/tls/latest")
                        .param("domainName",  "dnsbelgium.be")
                        .param("fetchLatest", "true"))
                .andExpect(view().name("visit-details-tls"))
                .andExpect(model().attributeExists( "domainName"));

    }

    @Test
    public void findLatest_whenTlsCrawlResultWithNullFullScanEntity() throws Exception {
        TlsCrawlResult tlsVisit1 = objectMother.tlsCrawlResultWithNullFullVersionEntityScan();
        when(tlsRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.ofNullable(tlsVisit1));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/tls/latest")
                        .param("domainName",  "dnsbelgium.be")
                        .param("fetchLatest", "true"))
                .andExpect(view().name("visit-details-tls"))
                .andExpect(model().attributeExists( "domainName"));
    }
}
