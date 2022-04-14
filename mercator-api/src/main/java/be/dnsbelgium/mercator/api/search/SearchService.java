package be.dnsbelgium.mercator.api.search;

import be.dnsbelgium.mercator.api.status.CrawlComponentStatusService;
import be.dnsbelgium.mercator.dispatcher.persistence.DispatcherEvent;
import be.dnsbelgium.mercator.dispatcher.persistence.DispatcherEventRepository;
import javassist.NotFoundException;
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
    private final Logger logger = LoggerFactory.getLogger(SearchService.class); // Removed most loggers. Generally used for debugging.

    private final CrawlComponentStatusService crawlComponentStatusService;
    private final DispatcherEventRepository dispatcherEventRepository;

    @Autowired
    public SearchService(CrawlComponentStatusService crawlComponentStatusService, DispatcherEventRepository dispatcherEventRepository) {
        this.crawlComponentStatusService = crawlComponentStatusService;
        this.dispatcherEventRepository = dispatcherEventRepository;
    }

    /**
     * Gets the all the necessary information needed for the frontend's search result table by pageNr.
     * Frontend calls a search by domain name, gets a PageDTO as the return.
     * @param domainName Requested Domain. F.ex.: abc.be
     * @param pageNumber Requested page within domainName.
     * @return PageDTO with a List of SearchDTO's (containing VisitId, ContentCrawler booleans),
     *         int amount of pages,
     *         hasNext & hasPrevious booleans.
     * @throws NotFoundException When a requested resource is not found in the database.
     */
    public PageDTO getPageForDomain(String domainName, int pageNumber) throws NotFoundException, ExecutionException, InterruptedException {
        logger.info("Searching for " + domainName);

        // Create page requested by pageNumber.
        Pageable paging = PageRequest.of(pageNumber, 10, Sort.by("requestTimestamp").descending());
        Page<DispatcherEvent> dispatcherPage = dispatcherEventRepository.findDispatcherEventByDomainName(domainName, paging);

        if (!dispatcherPage.hasContent()) {
            logger.info("Dispatcher has no content.");
            throw new NotFoundException(String.format("Domain %s was not yet crawled or does not exist.", domainName));
            // TODO: Return a code that the frontend will translate in the correct message
        }

        // Create PageDTO to return.
        PageDTO pageDTO = new PageDTO();
        pageDTO.setAmountOfRecords(dispatcherPage.getTotalElements());
        pageDTO.setAmountOfPages(dispatcherPage.getTotalPages());
        pageDTO.setHasNext(dispatcherPage.hasNext());
        pageDTO.setHasPrevious(dispatcherPage.hasPrevious());

        // Create a list of SearchDTO's to add to PageDTO.
        // SearchDTO's contain: VisitId, StatusBooleans (CrawlComponentStatus), FinalUrl.
        List<SearchDTO> dtoList = new ArrayList<>();

        // Create list of visitId's for DomainName from the requested page.
        for (DispatcherEvent event: dispatcherPage) {
            UUID vId = event.getVisitId();

            // Create DTO to add to dtoList.
            SearchDTO dto = new SearchDTO();
            dto.setVisitId(vId);
            dto.setRequestTimeStamp(event.getRequestTimestamp());
            // Get the Status Booleans by visitId
            dto.setCrawlStatus(crawlComponentStatusService.getCrawlComponentStatus(vId));

            dtoList.add(dto);
        }

        // Add list of SearchDTO's to PageDTO.
        pageDTO.setDtos(dtoList);

        logger.debug("Returning PageDTO containing a list of SearchDTO's.");

        return pageDTO;
    }

}
