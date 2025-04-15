package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.SearchVisitIdResultItem;
import be.dnsbelgium.mercator.persistence.SmtpRepository;
import be.dnsbelgium.mercator.smtp.dto.SmtpVisit;
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
public class SmtpSearchControllerTest {

    @MockitoBean
    private SmtpRepository smtpRepository;

    private final ObjectMother objectMother = new ObjectMother();

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void searchVisitIds_found() throws Exception {
        SearchVisitIdResultItem searchVisitIdResultItem1 = new SearchVisitIdResultItem("v101", Instant.now());
        SearchVisitIdResultItem searchVisitIdResultItem2 = new SearchVisitIdResultItem("v102", Instant.now());
        SearchVisitIdResultItem searchVisitIdResultItem3 = new SearchVisitIdResultItem("v103", Instant.now());
        when(smtpRepository.searchVisitIds("dnsbelgium.be")).thenReturn(List.of(searchVisitIdResultItem1,searchVisitIdResultItem2,searchVisitIdResultItem3));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/smtp/ids")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("search-results-smtp"))
                .andExpect(model().attributeExists( "visitIds"))
                .andExpect(content().string(containsString("v101")))
                .andExpect(content().string(containsString("v102")))
                .andExpect(content().string(containsString("v103")))
        ;

    }

    @Test
    public void searchVisitIds_notFound() throws Exception {
        when(smtpRepository.searchVisitIds("dnsbelgium.be")).thenReturn(List.of());
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/smtp/ids")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("search-results-smtp"))
                .andExpect(model().attributeExists("visitIds"))
                .andExpect(content().string(containsString("No SMTP crawls found for <span>dnsbelgium.be</span>")))
        ;
    }

    @Test
    public void findByVisitId_findsVisitDetails() throws Exception {
        SmtpVisit smtpVisitResult = objectMother.smtpVisit1();
        when(smtpRepository.findByVisitId("idjsfijoze-er-ze")).thenReturn(Optional.of(smtpVisitResult));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/smtp/id")
                        .param("visitId", "idjsfijoze-er-ze"))
                .andExpect(view().name("visit-details-smtp"))
                .andExpect(model().attributeExists( "smtpVisitResult"))
                .andExpect(content().string(containsString("STARTTLS")));
    }

    @Test
    public void findByVisitId_doesNotfindVisitDetails() throws Exception {
        when(smtpRepository.findByVisitId("idjsfijoze-er-ze")).thenReturn(Optional.empty());
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/smtp/id")
                        .param("visitId", "idjsfijoze-er-ze"))
                .andExpect(content().string(containsString("No SMTP crawl results found for visit-id <strong>idjsfijoze-er-ze</strong>")));
    }


    @Test
    public void findLatestResult_findsLatestVisitDetails() throws Exception {
        SmtpVisit smtpVisitResult = objectMother.smtpVisit1();
        when(smtpRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.ofNullable(smtpVisitResult));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/smtp/latest")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("visit-details-smtp"))
                .andExpect(model().attributeExists( "smtpVisitResult"))
                .andExpect(content().string(containsString("STARTTLS")));
    }

    @Test
    public void findLatestResult_whenSmtpVisitObjectHasNullValues() throws Exception {
        SmtpVisit smtpVisitResult = objectMother.smtpVisitWithNullValues();
        when(smtpRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.ofNullable(smtpVisitResult));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/smtp/latest")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("visit-details-smtp"))
                .andExpect(model().attributeExists( "smtpVisitResult"))
                .andExpect(content().string(containsString("No hosts")));
    }

    @Test
    public void findLatestResult_whenSmtpHostObjectHasNullValues() throws Exception {
        SmtpVisit smtpVisitResult = objectMother.smtpVisitWithNullValues2();
        when(smtpRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.ofNullable(smtpVisitResult));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/smtp/latest")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("visit-details-smtp"))
                .andExpect(model().attributeExists( "smtpVisitResult"))
                .andExpect(content().string(containsString("No conversation")));
    }

    @Test
    public void findLatestResult_whenSmtpConversationObjectHasNullValues() throws Exception {
        SmtpVisit smtpVisitResult = objectMother.smtpVisitWithNullValues3();
        when(smtpRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.ofNullable(smtpVisitResult));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/smtp/latest")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("visit-details-smtp"))
                .andExpect(model().attributeExists( "smtpVisitResult"));
    }



}