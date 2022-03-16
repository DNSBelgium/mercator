package be.dnsbelgium.mercator.content.domain;

import be.dnsbelgium.mercator.common.messaging.ack.AckMessageService;
import be.dnsbelgium.mercator.common.messaging.ack.CrawlerModule;
import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.content.domain.content.ContentResolutionTest;
import be.dnsbelgium.mercator.content.domain.content.ContentResolver;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResult;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResultRepository;
import be.dnsbelgium.mercator.content.dto.MuppetsResolution;
import be.dnsbelgium.mercator.content.persistence.WappalyzerResultRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringJUnitConfig({ContentCrawlService.class})
@TestPropertySource(properties = {"content.crawler.url.prefixes=http://www."})
class ContentCrawlServiceTest {

  @MockBean
  ContentResolver contentResolver;
  @MockBean
  ContentCrawlResultRepository contentCrawlResultRepository;
  @MockBean
  WappalyzerResultRepository wappalyzerResultRepository;
  @MockBean
  AckMessageService ackMessageService;

  @Value("${content.crawler.url.prefixes}")
  List<String> prefixes;

  @Autowired
  ContentCrawlService contentCrawlService;

  @Captor
  ArgumentCaptor<ContentCrawlResult> contentCrawlResultArgumentCaptor;

  @Test
  void retrieveContent() {
    VisitRequest visitRequest = new VisitRequest(UUID.randomUUID(), "dnsbelgium.be");
    contentCrawlService.retrieveContent(visitRequest);
    verify(contentResolver).requestContentResolving(visitRequest, List.of("http://www.dnsbelgium.be"));
  }

  @Test
  void contentRetrieved() {
    MuppetsResolution muppetsResolution = ContentResolutionTest.contentResolutionTest();
    when(contentCrawlResultRepository.countByVisitId(eq(muppetsResolution.getVisitId()))).thenReturn(Long.valueOf(prefixes.size()));

    contentCrawlService.contentRetrieved(muppetsResolution);
    verify(contentCrawlResultRepository).saveAndIgnoreDuplicateKeys(contentCrawlResultArgumentCaptor.capture());
    ContentCrawlResult captured = contentCrawlResultArgumentCaptor.getValue();
    ContentCrawlResultTest.assertContentResolutionIsEqualToContentCrawlResult(muppetsResolution, captured);
    verify(ackMessageService).sendAck(captured.getVisitId(), captured.getDomainName(), CrawlerModule.MUPPETS);
  }

}
