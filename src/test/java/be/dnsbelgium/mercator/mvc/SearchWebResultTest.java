package be.dnsbelgium.mercator.mvc;

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

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = "be.dnsbelgium.mercator.mvc")
public class SearchWebResultTest {

    @MockitoBean
    private WebRepository webRepository;

    private final ObjectMother objectMother = new ObjectMother();

    @Autowired
    private MockMvc mockMvc;

    // This tests the html that is returned from the endpoint
    @Test
    public void getWebIds_findsIds() throws Exception {
        when(webRepository.searchVisitIds("dnsbelgium.be")).thenReturn(List.of("v101", "v102", "v103"));
        // Might need to be modified in the future to contain more data than just Id's
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/web/ids")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("search-results-web"))
                .andExpect(model().attributeExists( "visitIds"))
                .andExpect(content().string(containsString("v101")))
        ;

    }

    @Test
    public void getWeb_findsVisitDetails() throws Exception {
        WebCrawlResult webCrawlResult1 = objectMother.webCrawlResult1();
        when(webRepository.findByVisitId("idjsfijoze-er-ze")).thenReturn(Optional.ofNullable(webCrawlResult1));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/web/id")
                        .param("visitId", "idjsfijoze-er-ze"))
                .andExpect(view().name("visit-details-web"))
                .andExpect(model().attributeExists( "webCrawlResults"))
                .andExpect(content().string(containsString("dnsbelgium.be")));
    }


    @Test
    public void getWebLatest_findsLatestVisitDetails() throws Exception {
        WebCrawlResult webCrawlResult1 = objectMother.webCrawlResult1();
        when(webRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.ofNullable(webCrawlResult1));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/web/latest")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("visit-details-web"))
                .andExpect(model().attributeExists( "webCrawlResults"))
                .andExpect(content().string(containsString("dnsbelgium.be")));
    }

}