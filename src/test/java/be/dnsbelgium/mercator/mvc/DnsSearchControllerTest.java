package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.dns.dto.DnsCrawlResult;
import be.dnsbelgium.mercator.persistence.BaseRepository;
import be.dnsbelgium.mercator.persistence.DnsRepository;
import be.dnsbelgium.mercator.test.ObjectMother;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = "be.dnsbelgium.mercator.mvc")
public class DnsSearchControllerTest {

    @MockitoBean
    private DnsRepository dnsRepository;

    private final ObjectMother objectMother = new ObjectMother();

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void searchVisitIds_found() throws Exception {
        BaseRepository.SearchVisitIdResultItem searchVisitIdResultItem1 = new BaseRepository.SearchVisitIdResultItem("v101", Instant.now());
        BaseRepository.SearchVisitIdResultItem searchVisitIdResultItem2 = new BaseRepository.SearchVisitIdResultItem("v102", Instant.now());
        BaseRepository.SearchVisitIdResultItem searchVisitIdResultItem3 = new BaseRepository.SearchVisitIdResultItem("v103", Instant.now());
        when(dnsRepository.searchVisitIds("dnsbelgium.be")).thenReturn(List.of(searchVisitIdResultItem1,searchVisitIdResultItem2,searchVisitIdResultItem3));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/dns/ids")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("search-results-dns"))
                .andExpect(model().attributeExists( "visitIds"))
                .andExpect(content().string(containsString("v101")))
                .andExpect(content().string(containsString("v102")))
                .andExpect(content().string(containsString("v103")))
        ;

    }

    @Test
    public void searchVisitIds_notFound() throws Exception {
        when(dnsRepository.searchVisitIds("dnsbelgium.be")).thenReturn(List.of());
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/dns/ids")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("search-results-dns"))
                .andExpect(model().attributeExists("visitIds"))
                .andExpect(content().string(containsString("No visitIds found for dns of")))
        ;
    }

    @Test
    public void findByVisitId_findsVisitDetails() throws Exception {
        DnsCrawlResult dnsCrawlResult = objectMother.dnsCrawlResultWithMultipleResponses();
        when(dnsRepository.findByVisitId("idjsfijoze-er-ze")).thenReturn(Optional.of(dnsCrawlResult));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/dns/id")
                        .param("visitId", "idjsfijoze-er-ze"))
                .andExpect(view().name("visit-details-dns"))
                .andExpect(model().attributeExists( "dnsCrawlResult"))
                .andExpect(content().string(containsString("192.168.1.1")))
                .andExpect(content().string(containsString("ISP Belgium")))
                .andExpect(content().string(containsString("2025-03-28T12:00:00Z")))
                .andExpect(content().string(containsString("2")))
                .andExpect(content().string(containsString("A")));;
    }

    @Test
    public void findByVisitId_doesNotfindVisitDetails() throws Exception {
        when(dnsRepository.findByVisitId("idjsfijoze-er-ze")).thenReturn(Optional.empty());
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/dns/id")
                        .param("visitId", "idjsfijoze-er-ze"))
                .andExpect(content().string(containsString("No dns crawl result found for visitId")));
    }


    @Test
    public void findLatestResult_findsLatestVisitDetails() throws Exception {
        DnsCrawlResult dnsCrawlResult = objectMother.dnsCrawlResultWithMultipleResponses();
        when(dnsRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.ofNullable(dnsCrawlResult));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/dns/latest")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("visit-details-dns"))
                .andExpect(model().attributeExists( "dnsCrawlResult"))
                .andExpect(content().string(containsString("192.168.1.1")))
                .andExpect(content().string(containsString("ISP Belgium")))
                .andExpect(content().string(containsString("2025-03-28T12:00:00Z")))
                .andExpect(content().string(containsString("2")))
                .andExpect(content().string(containsString("A")));
    }

}