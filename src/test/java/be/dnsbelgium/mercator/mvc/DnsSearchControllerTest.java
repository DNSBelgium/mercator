package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.common.VisitIdGenerator;
import be.dnsbelgium.mercator.dns.dto.DnsCrawlResult;
import be.dnsbelgium.mercator.persistence.DnsRepository;
import be.dnsbelgium.mercator.persistence.SearchVisitIdResultItem;
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
        SearchVisitIdResultItem searchVisitIdResultItem1 = new SearchVisitIdResultItem("v101", Instant.now());
        SearchVisitIdResultItem searchVisitIdResultItem2 = new SearchVisitIdResultItem("v102", Instant.now());
        SearchVisitIdResultItem searchVisitIdResultItem3 = new SearchVisitIdResultItem("v103", Instant.now());
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
                .andExpect(content().string(containsString("No DNS crawls found for <span>dnsbelgium.be</span>")))
        ;
    }

    @Test
    public void findByVisitId_findsVisitDetails() throws Exception {
        String visitId = VisitIdGenerator.generate();
        DnsCrawlResult dnsCrawlResultResult1 = objectMother.dnsCrawlResultWithMultipleResponses1("dnsbelgium.be", visitId);
        when(dnsRepository.findByVisitId(visitId)).thenReturn(Optional.of(dnsCrawlResultResult1));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/dns/id")
                        .param("visitId", visitId))
                .andExpect(view().name("visit-details-dns"))
                .andExpect(model().attributeExists( "dnsCrawlResult"))
                .andExpect(content().string(containsString("192.168.1.1")))
                .andExpect(content().string(containsString("ISP Belgium")))
                .andExpect(content().string(containsString("2025-03-28 13:00:00 CET")))
                .andExpect(content().string(containsString("2")))
                .andExpect(content().string(containsString("A")));
    }

    @Test
    public void findByVisitId_doesNotFindVisitDetails() throws Exception {
        String visitId = VisitIdGenerator.generate();
        when(dnsRepository.findByVisitId(visitId)).thenReturn(Optional.empty());
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/dns/id")
                        .param("visitId", visitId))
                .andExpect(content().string(containsString("No DNS crawl results found for visit-id <strong>" + visitId + "</strong>")));
    }


    @Test
    public void findLatestResult_findsLatestVisitDetails() throws Exception {
        DnsCrawlResult dnsCrawlResultResult1 = objectMother.dnsCrawlResultWithMultipleResponses1("dnsbelgium.be", "1");
        when(dnsRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.ofNullable(dnsCrawlResultResult1));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/dns/latest")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("visit-details-dns"))
                .andExpect(model().attributeExists( "dnsCrawlResult"))
                .andExpect(content().string(containsString("192.168.1.1")))
                .andExpect(content().string(containsString("ISP Belgium")))
                .andExpect(content().string(containsString("2025-03-28 13:00:00 CET")))
                .andExpect(content().string(containsString("2")))
                .andExpect(content().string(containsString("A")));
    }

    @Test
    public void findLatest_whenDnsCrawlResultWithNullValues() throws Exception {
        DnsCrawlResult dnsCrawlResult = objectMother.dnsCrawlResultWithNullValues();

        when(dnsRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.ofNullable(dnsCrawlResult));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/dns/latest")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("visit-details-dns"))
                .andExpect(model().attributeExists("dnsCrawlResult"));
    }

    @Test
    public void findLatest_whenDnsCrawlResultWithRequestNullValues() throws Exception {
        DnsCrawlResult dnsCrawlResult = objectMother.dnsCrawlResultWithNullRequest();

        when(dnsRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.ofNullable(dnsCrawlResult));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/dns/latest")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("visit-details-dns"))
                .andExpect(model().attributeExists("dnsCrawlResult"));
    }

    @Test
    public void findLatest_whenDnsCrawlResultWithResponseNullValues() throws Exception {
        DnsCrawlResult dnsCrawlResult = objectMother.dnsCrawlResultWithNullResponse();

        when(dnsRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.ofNullable(dnsCrawlResult));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/dns/latest")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("visit-details-dns"))
                .andExpect(model().attributeExists("dnsCrawlResult"));
    }

    @Test
    public void findLatest_whenDnsCrawlResultWithGeoIpNullValues() throws Exception {
        DnsCrawlResult dnsCrawlResult = objectMother.dnsCrawlResultWithNullGeoIp();

        when(dnsRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.ofNullable(dnsCrawlResult));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/dns/latest")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("visit-details-dns"))
                .andExpect(model().attributeExists("dnsCrawlResult"));
    }

}