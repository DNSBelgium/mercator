package be.dnsbelgium.mercator.api.search;

import be.dnsbelgium.mercator.api.status.CrawlComponentStatus;
import be.dnsbelgium.mercator.api.status.CrawlComponentStatusService;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResult;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResultRepository;
import be.dnsbelgium.mercator.dispatcher.persistence.DispatcherEvent;
import be.dnsbelgium.mercator.dispatcher.persistence.DispatcherEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.ZonedDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(SearchService.class)
class SearchServiceTest {

    @MockBean
    DispatcherEventRepository dispatcherEventRepository;
    @MockBean
    CrawlComponentStatusService crawlComponentStatusService;
    @MockBean
    ContentCrawlResultRepository contentCrawlResultRepository;

    @Autowired
    SearchService searchService;

    Page<DispatcherEvent> dispatcherPage;
    String domainName = "dnsbelgium.be";
    String labelName = "some-label";
    String screenshotKey = "http://some-screen-shot-key.be";
    ZonedDateTime now = ZonedDateTime.now();
    ContentCrawlResult contentCrawlResult = new ContentCrawlResult();

    @BeforeEach
    void setup () {
        List<DispatcherEvent> dispatcherList = new ArrayList<>();
        DispatcherEvent dispatcherEvent = new DispatcherEvent();
        dispatcherEvent.setVisitId(UUID.randomUUID());
        dispatcherEvent.setDomainName(domainName);
        dispatcherEvent.setRequestTimestamp(now);
        dispatcherEvent.setLabels(List.of(labelName));

        dispatcherList.add(dispatcherEvent);

        dispatcherEvent = new DispatcherEvent();
        dispatcherEvent.setVisitId(UUID.randomUUID());
        dispatcherEvent.setDomainName(domainName);
        dispatcherEvent.setRequestTimestamp(now);
        dispatcherEvent.setLabels(List.of(labelName));

        dispatcherList.add(dispatcherEvent);

        dispatcherEvent = new DispatcherEvent();
        dispatcherEvent.setVisitId(UUID.randomUUID());
        dispatcherEvent.setDomainName(domainName);
        dispatcherEvent.setRequestTimestamp(now);
        dispatcherEvent.setLabels(List.of(labelName));

        dispatcherList.add(dispatcherEvent);

        dispatcherPage = new PageImpl<>(dispatcherList);

        contentCrawlResult.setScreenshotKey(screenshotKey);
    }

    @Test
    void getPageForDomain() throws Exception {
        when(dispatcherEventRepository.findDispatcherEventByDomainName(any(String.class), any(PageRequest.class))).thenReturn(
                dispatcherPage
        );
        when(crawlComponentStatusService.getCrawlComponentStatus(any(UUID.class))).thenReturn(
                new CrawlComponentStatus(UUID.randomUUID(), true, true, true, true)
        );
        when(contentCrawlResultRepository.findByVisitId(any(UUID.class))).thenReturn(
                List.of(contentCrawlResult)
        );

        PageDTO pageDTO = searchService.getPageForDomain(domainName, 1);

        assertThat(pageDTO.getAmountOfRecords()).isEqualTo(3);
        assertThat(pageDTO.getAmountOfPages()).isEqualTo(1);
        assertThat(pageDTO.isHasNext()).isEqualTo(false);
        assertThat(pageDTO.isHasPrevious()).isEqualTo(false);
        assertThat(pageDTO.getDtos().size()).isEqualTo(3);

        for (SearchDTO dto : pageDTO.getDtos()) {
            assertThat(dto.getDomainName()).isEqualTo(domainName);
            assertThat(dto.getRequestTimeStamp()).isEqualTo(now);
            assertThat(dto.getCrawlStatus().isDns()).isTrue();
            assertThat(dto.getCrawlStatus().isSmtp()).isTrue();
            assertThat(dto.getCrawlStatus().isMuppets()).isTrue();
            assertThat(dto.getCrawlStatus().isWappalyzer()).isTrue();
            assertThat(dto.getScreenshotKey()).isEqualTo(screenshotKey);
        }
    }

}