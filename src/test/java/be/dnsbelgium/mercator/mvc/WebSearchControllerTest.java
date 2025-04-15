package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.common.VisitIdGenerator;
import be.dnsbelgium.mercator.persistence.SearchVisitIdResultItem;
import be.dnsbelgium.mercator.persistence.WebRepository;
import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = "be.dnsbelgium.mercator.mvc")
public class WebSearchControllerTest {

    @MockitoBean
    private WebRepository webRepository;

    private final ObjectMother objectMother = new ObjectMother();

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getWebIds_findsIds() throws Exception {
        when(webRepository.searchVisitIds("dnsbelgium.be")).thenReturn(Stream.of("v101", "v102", "v103").map(v -> new SearchVisitIdResultItem(v, null)).toList());
        // Might need to be modified in the future to contain more data than just Id's
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/web/ids")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("search-results-web"))
                .andExpect(model().attributeExists( "visitIds"))
                .andExpect(content().string(containsString("v101")))
                .andExpect(content().string(containsString("v102")))
                .andExpect(content().string(containsString("v103")))
        ;

    }

    @Test
    public void searchVisitIds_doesNotfindIds() throws Exception {
        when(webRepository.searchVisitIds("dnsbelgium.be")).thenReturn(List.of());
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/web/ids")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("search-results-web"))
                .andExpect(model().attributeExists( "visitIds"))
                .andExpect(content().string(containsString("No web crawls found for <span>dnsbelgium.be</span>")))
        ;

    }

    @Test
    public void findByVisitId_findsVisitDetails() throws Exception {
        WebCrawlResult webCrawlResult1 = objectMother.webCrawlResult1();
        when(webRepository.findByVisitId("idjsfijoze-er-ze")).thenReturn(Optional.ofNullable(webCrawlResult1));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/web/id")
                        .param("visitId", "idjsfijoze-er-ze"))
                .andExpect(view().name("visit-details-web"))
                .andExpect(model().attributeExists( "webCrawlResult"))
                .andExpect(content().string(containsString("dnsbelgium.be")))
                .andExpect(content().string(containsString("Google Tag Manager")));
    }

    @Test
    public void findByVisitId_doesNotfindVisitDetails() throws Exception {
        String visitId = VisitIdGenerator.generate();
        when(webRepository.findByVisitId(visitId)).thenReturn(Optional.empty());
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/web/id")
                        .param("visitId", visitId))
                .andExpect(view().name("visit-details-web"))
                .andExpect(content().string(containsString("No web crawl results found for visit-id <strong>" + visitId + "</strong>")));
    }


    @Test
    public void findLatestResult_findsLatestVisitDetails() throws Exception {
        WebCrawlResult webCrawlResult1 = objectMother.webCrawlResult1();
        when(webRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.of(webCrawlResult1));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/web/latest")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("visit-details-web"))
                .andExpect(model().attributeExists( "webCrawlResult"))
                .andExpect(content().string(containsString("dnsbelgium.be")))
                .andExpect(content().string(containsString("Google Tag Manager")));
    }

    @Test
    public void findLatestResultWithSecurityTxt_findsLatestVisitDetails() throws Exception {
        WebCrawlResult webCrawlResult1 = objectMother.webCrawlResultWithPageVisitWithSecurityTxt();

        when(webRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.of(webCrawlResult1));

        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/web/latest")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("visit-details-web"))
                .andExpect(model().attributeExists("webCrawlResult"))
                .andExpect(content().string(containsString("https://www.example.org/.well-known/security.txt")))
                .andExpect(content().string(containsString("Content-Type")))
                .andExpect(content().string(containsString("text/plain")))
                .andExpect(content().string(containsString("128")));
    }

    @Test
    public void findLatestResultWithRobotsTxt_findsLatestVisitDetails() throws Exception {
        WebCrawlResult webCrawlResult1 = objectMother.webCrawlResultWithPageVisitWithRobotsTxt();

        when(webRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.of(webCrawlResult1));

        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/web/latest")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("visit-details-web"))
                .andExpect(model().attributeExists("webCrawlResult"))
                .andExpect(content().string(containsString("# # robots.txt # # This file is to prevent the crawling and indexing of certain parts #")))
                .andExpect(content().string(containsString("Content-Type")))
                .andExpect(content().string(containsString("text/plain")))
                .andExpect(content().string(containsString("128")));
    }

    @Test
    public void findLatest_whenWebCrawlResultHasNullValues() throws Exception {
        WebCrawlResult webCrawlResult1 = objectMother.webCrawlResultWithNullValues();

        when(webRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.of(webCrawlResult1));

        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/web/latest")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("visit-details-web"))
                .andExpect(model().attributeExists("webCrawlResult"));
    }

    @Test
    public void findLatest_whenWebCrawlResultHtmlFeaturesHasNullValues() throws Exception {
        WebCrawlResult webCrawlResult1 = objectMother.webCrawlResultWithHtmlFeaturesNullValues();

        when(webRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.of(webCrawlResult1));

        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/web/latest")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("visit-details-web"))
                .andExpect(model().attributeExists("webCrawlResult"));
    }

    @Test
    public void findLatest_whenWebCrawlResultPageVisitHasNullValues() throws Exception {
        WebCrawlResult webCrawlResult1 = objectMother.webCrawlResultWithPageVisitNullValues();

        when(webRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.of(webCrawlResult1));

        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/web/latest")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("visit-details-web"))
                .andExpect(model().attributeExists("webCrawlResult"));
    }


}