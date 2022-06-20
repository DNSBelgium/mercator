package be.dnsbelgium.mercator.api.search;

import be.dnsbelgium.mercator.api.status.CrawlComponentStatusService;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResult;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResultRepository;
import be.dnsbelgium.mercator.dispatcher.persistence.DispatcherEvent;
import be.dnsbelgium.mercator.dispatcher.persistence.DispatcherEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ExecutionException;

@Service
public class SearchService {
    private final Logger logger = LoggerFactory.getLogger(SearchService.class);

    private final CrawlComponentStatusService crawlComponentStatusService;
    private final DispatcherEventRepository dispatcherEventRepository;
    private final ContentCrawlResultRepository contentCrawlResultRepository;

    @Autowired
    public SearchService(CrawlComponentStatusService crawlComponentStatusService, DispatcherEventRepository dispatcherEventRepository, ContentCrawlResultRepository contentCrawlResultRepository) {
        this.crawlComponentStatusService = crawlComponentStatusService;
        this.dispatcherEventRepository = dispatcherEventRepository;
        this.contentCrawlResultRepository = contentCrawlResultRepository;
    }

    /**
     * Gets information by domain name and page number.
     * @param domainName Requested domain name to search for. F.ex.: abc.be
     * @param pageNumber Requested page number within the search.
     * @return PageDTO with a List of SearchDTO's (containing VisitId, ContentCrawler booleans),
     *         int amount of pages,
     *         hasNext & hasPrevious booleans.
     * @throws NoSuchElementException When a requested resource is not found in the database.
     */
    public PageDTO getPageForDomain(String domainName, int pageNumber) throws NoSuchElementException, ExecutionException, InterruptedException {
        logger.info("Searching for domain name: {}", domainName);

        // Create page requested by pageNumber.
        Pageable paging = PageRequest.of(pageNumber, 10, Sort.by("requestTimestamp").descending());
        Page<DispatcherEvent> dispatcherPage = dispatcherEventRepository.findDispatcherEventByDomainName(domainName, paging);

        if (!dispatcherPage.hasContent()) {
            logger.info("Dispatcher has no content.");
            throw new NoSuchElementException(String.format("Domain %s was not yet crawled or does not exist.", domainName));
            // TODO: Return a code that the frontend will translate in the correct message
        }

        return pageToPageDTO(dispatcherPage);
    }

    /**
     * Transforms a Page<DispatcherEvent> to a PageDTO.
     * @param dispatcherPage to transform to DTO.
     * @return PageDTO containing a list of SearchDTO's.
     */
    private PageDTO pageToPageDTO(Page<DispatcherEvent> dispatcherPage) throws ExecutionException, InterruptedException {
        // Create PageDTO to return.
        PageDTO pageDTO = new PageDTO();
        pageDTO.setAmountOfRecords(dispatcherPage.getTotalElements());
        pageDTO.setAmountOfPages(dispatcherPage.getTotalPages());
        pageDTO.setHasNext(dispatcherPage.hasNext());
        pageDTO.setHasPrevious(dispatcherPage.hasPrevious());

        // Create a list of SearchDTO's to add to PageDTO.
        // SearchDTO's contain: VisitId, StatusBooleans (CrawlComponentStatus), FinalUrl.
        List<SearchDTO> searchDTOList = new ArrayList<>();

        // Create list of visitId's for DomainName from the requested page.
        for (DispatcherEvent event: dispatcherPage) {
            UUID vId = event.getVisitId();

            // Create DTO to add to searchDtoList.
            SearchDTO dto = new SearchDTO();
            dto.setVisitId(vId);
            dto.setDomainName(event.getDomainName());
            dto.setRequestTimeStamp(event.getRequestTimestamp());
            // Get the Status Booleans by visitId.
            dto.setCrawlStatus(crawlComponentStatusService.getCrawlComponentStatus(vId));

            // Setting screenshotKey.
            List<ContentCrawlResult> contentResults = contentCrawlResultRepository.findByVisitId(vId);
            if (!contentResults.isEmpty()) {
                Optional<ContentCrawlResult> resultWithKey = contentResults.stream().filter(r -> r.getScreenshotKey() != null).findFirst();
                resultWithKey.ifPresent(contentCrawlResult -> dto.setScreenshotKey(contentCrawlResult.getScreenshotKey()));
            }

            searchDTOList.add(dto);
        }

        pageDTO.setDtos(searchDTOList);
        return pageDTO;
    }

}
