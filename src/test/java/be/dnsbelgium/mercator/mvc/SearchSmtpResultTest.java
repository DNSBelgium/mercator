package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.SmtpRepository;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
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

@SpringBootTest
@AutoConfigureMockMvc
@ComponentScan(basePackages = "be.dnsbelgium.mercator.mvc")
public class SearchSmtpResultTest {

    @MockitoBean
    private SmtpRepository smtpRepository;

    private final ObjectMother objectMother = new ObjectMother();

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getSmtpIds_findsIds() throws Exception {
        when(smtpRepository.searchVisitIds("dnsbelgium.be")).thenReturn(List.of("v101", "v102", "v103"));
        // Might need to be modified in the future to contain more data than just Id's
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/smtp/ids")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("search-results-smtp"))
                .andExpect(model().attributeExists( "visitIds"))
                .andExpect(content().string(containsString("v101")))
        ;

    }

    @Test
    public void getSmtpIds_doesNotfindIds() throws Exception {
        when(smtpRepository.searchVisitIds("dnsbelgium.be")).thenReturn(List.of());
        // Might need to be modified in the future to contain more data than just Id's
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/smtp/ids")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("search-results-smtp"))
                .andExpect(model().attributeExists("visitIds"))
                .andExpect(content().string(containsString("No visitIds found for smtp of this domain")))
        ;
    }

    @Test
    public void getSmtp_findsVisitDetails() throws Exception {
        SmtpConversation smtpConversation1 = objectMother.smtpConversation1();
        when(smtpRepository.findByVisitId("idjsfijoze-er-ze")).thenReturn(Optional.ofNullable(smtpConversation1));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/smtp/id")
                        .param("visitId", "idjsfijoze-er-ze"))
                .andExpect(view().name("visit-details-smtp"))
                .andExpect(model().attributeExists( "smtpConversationResults"))
                .andExpect(content().string(containsString("software host2")));
    }

    @Test
    public void getSmtp_doesNotfindVisitDetails() throws Exception {
        WebCrawlResult webCrawlResult1 = objectMother.webCrawlResult1();
        when(smtpRepository.findByVisitId("idjsfijoze-er-ze")).thenReturn(Optional.empty());
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/smtp/id")
                        .param("visitId", "idjsfijoze-er-ze"))
                .andExpect(content().string(containsString("No SMTP conversations results found for visitId")));
    }


    @Test
    public void getSmtpLatest_findsLatestVisitDetails() throws Exception {
        SmtpConversation smtpConversation = objectMother.smtpConversation1();
        when(smtpRepository.findLatestResult("dnsbelgium.be")).thenReturn(Optional.ofNullable(smtpConversation));
        this.mockMvc
                .perform(MockMvcRequestBuilders.get("/search/smtp/latest")
                        .param("domainName", "dnsbelgium.be"))
                .andExpect(view().name("visit-details-smtp"))
                .andExpect(model().attributeExists( "smtpConversationResults"))
                .andExpect(content().string(containsString("software host2")));
    }

}
