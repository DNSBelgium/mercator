package be.dnsbelgium.mercator.smtp.ports;

import be.dnsbelgium.mercator.common.messaging.ack.AckMessageService;
import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.smtp.SmtpCrawlService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.autoconfigure.metrics.CompositeMeterRegistryAutoConfiguration;
import org.springframework.boot.actuate.autoconfigure.metrics.MetricsAutoConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@SpringJUnitConfig({SmtpCrawler.class, MetricsAutoConfiguration.class,
                    CompositeMeterRegistryAutoConfiguration.class})
class SmtpCrawlerTest {

    @Autowired SmtpCrawler crawler;

    @MockBean SmtpCrawlService service;

    @MockBean AckMessageService ackMessageService;

    @Test
    public void nullRequestIsIgnored() throws Exception {
        crawler.process(null);
        verify(service, never()).retrieveSmtpInfo(any(VisitRequest.class));
    }

    @Test
    public void missingDomainNameIsIgnored() throws Exception {
        VisitRequest request = new VisitRequest(UUID.randomUUID(), null);
        crawler.process(request);
        verify(service, never()).retrieveSmtpInfo(any(VisitRequest.class));
    }

    @Test
    public void missingVisitIdIsIgnored() throws Exception {
        VisitRequest request = new VisitRequest(null, "abc.be");
        crawler.process(request);
        verify(service, never()).retrieveSmtpInfo(any(VisitRequest.class));
    }

    @Test
    public void happyPath() throws Exception {
        VisitRequest request = new VisitRequest(UUID.randomUUID(), "abc.be");
        crawler.process(request);
        verify(service, times(1)).retrieveSmtpInfo(request);
    }

    @Test
    public void oneFailure() throws Exception {
        VisitRequest request = new VisitRequest(UUID.randomUUID(), "abc.be");
        doThrow(new RuntimeException("first failure")).when(service).retrieveSmtpInfo(request);
        assertThrows(RuntimeException.class, () -> crawler.process(request));
        verify(service, times(1)).retrieveSmtpInfo(request);
    }

}
