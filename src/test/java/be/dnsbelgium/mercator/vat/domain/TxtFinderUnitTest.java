package be.dnsbelgium.mercator.vat.domain;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.vat.WebCrawler;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@SpringBootTest
public class TxtFinderUnitTest {

    ObjectMother objectMother = new ObjectMother();

    @InjectMocks
    private WebCrawler mockedWebCrawler;

    @Mock
    private VatScraper mockedVatScraper;



    @Test
    public void testFindSecurityTxt_notFound() {
        VisitRequest visitRequest = new VisitRequest("test-id", "anything.be");
        HttpUrl securityTxtUrl = HttpUrl.parse("https://www.anything.be/.well-known/security.txt");
        Page page1 = objectMother.page1();
        when(mockedVatScraper.fetchAndParse(securityTxtUrl)).thenReturn(page1);

        PageVisit result = mockedWebCrawler.findSecurityTxt(visitRequest);

        assertThat(result.getStatusCode()).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(400);
        verify(mockedVatScraper, times(1)).fetchAndParse(securityTxtUrl);
    }

    @Test
    public void testFindSecurityTxt_found() {
        VisitRequest visitRequest = new VisitRequest("test-id", "anything.be");
        HttpUrl securityTxtUrl = HttpUrl.parse("https://www.anything.be/.well-known/security.txt");
        Page page2 = objectMother.page2();
        when(mockedVatScraper.fetchAndParse(securityTxtUrl)).thenReturn(page2);

        PageVisit result = mockedWebCrawler.findSecurityTxt(visitRequest);

        assertThat(result.getStatusCode()).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(200);
        verify(mockedVatScraper, times(1)).fetchAndParse(securityTxtUrl);
    }

    @Test
    public void testFindRobotsTxt_notFound() {
        VisitRequest visitRequest = new VisitRequest("test-id", "anything.be");
        HttpUrl robotsTxtUrl = HttpUrl.parse("https://www.anything.be/robots.txt");
        Page page1 = objectMother.page1();
        when(mockedVatScraper.fetchAndParse(robotsTxtUrl)).thenReturn(page1);

        PageVisit result = mockedWebCrawler.findRobotsTxt(visitRequest);

        assertThat(result.getStatusCode()).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(400);
        verify(mockedVatScraper, times(1)).fetchAndParse(robotsTxtUrl);
    }

    @Test
    public void testFindRobotsTxt_found() {
        VisitRequest visitRequest = new VisitRequest("test-id", "anything.be");
        HttpUrl robotsTxtUrl = HttpUrl.parse("https://www.anything.be/robots.txt");
        Page page2 = objectMother.page2();
        when(mockedVatScraper.fetchAndParse(robotsTxtUrl)).thenReturn(page2);

        PageVisit result = mockedWebCrawler.findRobotsTxt(visitRequest);

        assertThat(result.getStatusCode()).isNotNull();
        assertThat(result.getStatusCode()).isEqualTo(200);
        verify(mockedVatScraper, times(1)).fetchAndParse(robotsTxtUrl);
    }


}
