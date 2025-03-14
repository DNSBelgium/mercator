package be.dnsbelgium.mercator.vat.domain;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.vat.WebCrawler;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Instant;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.*;

@SpringBootTest
public class SecurityTxtFinderTest {



    private static final Logger logger = LoggerFactory.getLogger(SecurityTxtFinderTest.class);


    ObjectMother objectMother = new ObjectMother();

    @Mock
    private VatScraper vatScraper;

    @InjectMocks
    private WebCrawler webCrawler;


    @Test
    @Disabled // this test makes an internet connection, use method to test if the method works
    public void testFindSecurityTxt() {
        VisitRequest visitRequest = new VisitRequest("sdiofjosidjf-sdfoijsodijf1564d5", "dnsbelgium.be");

        HttpUrl url = HttpUrl.parse("https://www.dnsbelgium.be");

        PageVisit updatedVisit = webCrawler.findSecurityTxt(url, visitRequest);
        logger.info(updatedVisit.getSecurity_txt_content());
        logger.info(updatedVisit.getSecurity_txt_url());
        logger.info(updatedVisit.getSecurity_txt_response_headers().toString());
        logger.info(String.valueOf(updatedVisit.getSecurity_txt_bytes()));

    }

    @Test
    public void testFindSecurityTxtLargerThan32kb() {
        VisitRequest visitRequest = new VisitRequest("test-id", "dnsbelgium.be");
        HttpUrl url = HttpUrl.parse("https://www.dnsbelgium.be");
        HttpUrl securityTxtUrl = HttpUrl.parse("https://www.dnsbelgium.be/.well-known/security.txt");

        String largeContent = "A".repeat(33000);

        Page mockPage = new Page(
                securityTxtUrl, Instant.now(), Instant.now(), 200, largeContent,
                33000, null, Collections.emptyMap()
        );

        when(vatScraper.fetchAndParse(securityTxtUrl)).thenReturn(mockPage);

        PageVisit result = webCrawler.findSecurityTxt(url, visitRequest);

        assertNull(result, "Expect null when security.txt is larger than 32 KB");

        verify(vatScraper, times(1)).fetchAndParse(securityTxtUrl);
    }

    @Test
    public void testFindSecurityTxt_notFound() {
        VisitRequest visitRequest = new VisitRequest("test-id", "dnsbelgium.be");
        HttpUrl url = HttpUrl.parse("https://www.dnsbelgium.be");
        HttpUrl securityTxtUrl = HttpUrl.parse("https://www.dnsbelgium.be/.well-known/security.txt");

        when(vatScraper.fetchAndParse(securityTxtUrl)).thenReturn(null);

        PageVisit result = webCrawler.findSecurityTxt(url, visitRequest);

        assertNull(result, "Expect null when security.txt is not found");
        verify(vatScraper, times(1)).fetchAndParse(securityTxtUrl);
    }





}
