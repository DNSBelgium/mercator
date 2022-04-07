package be.dnsbelgium.mercator.api.search;

import be.dnsbelgium.mercator.api.status.CrawlComponentStatus;
import be.dnsbelgium.mercator.api.status.CrawlComponentStatusController;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResultRepository;
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

@Service
public class SearchService {
    private final Logger logger = LoggerFactory.getLogger(SearchService.class); // Removed most loggers. Generally used for debugging.

    private final CrawlComponentStatusController crawlCompController;
    private final ContentCrawlResultRepository crawlRepo;
    private final DispatcherEventRepository dispatchRepo;

    @Autowired
    public SearchService(CrawlComponentStatusController crawlCompController, ContentCrawlResultRepository crawlRepo, DispatcherEventRepository dispatchRepo) {
        this.crawlCompController = crawlCompController;
        this.crawlRepo = crawlRepo;
        this.dispatchRepo = dispatchRepo;
    }

    /**
     * Gets the all the necessary information needed for the frontend's search result table by pageNr.
     * Frontend calls a search by domain name, gets a PageDTO as the return.
     * @param domainName Requested Domain. F.ex.: abc.be
     * @param pageNumber Requested page within domainName.
     * @return PageDTO with a List of SearchDTO's (containing VisitId, FinalURL, ContentCrawler booleans),
     *         int amount of pages,
     *         hasNext & hasPrevious booleans.
     * @throws NotFoundException When a requested resource is not found in the database.
     */
    public PageDTO getPageForDomain(String domainName, int pageNumber) throws NotFoundException {
        logger.info("Searching for " + domainName);

        // Create page requested by pageNumber.
        Pageable paging = PageRequest.of(pageNumber, 10, Sort.by("requestTimestamp").descending());
        Page<DispatcherEvent> dispatcherPage = dispatchRepo.findDispatcherEventByDomainName(domainName, paging);

        if (!dispatcherPage.hasContent()) {
            throw new NotFoundException(String.format("Domain %s was not yet crawled or does not exist.", domainName));
        }

        // Create list of visitId's for DomainName from the requested page.
        List<UUID> visitIds = new ArrayList<>();
        for (DispatcherEvent event: dispatcherPage) {
            visitIds.add(event.getVisitId());
        }

        // Get the finalUrls for the visitId's
        // Then get the list of Status booleans for the found visitId's
        Map<UUID, String> urlMap = new HashMap();
        List<CrawlComponentStatus> statusBools = new ArrayList<>();
        for (UUID id: visitIds) {

            // Get optional URL by visitId or else throw NotFoundException.
            String finalUrl = crawlRepo.getUrlByVisitId(id).orElseThrow(() ->
                    new NotFoundException(String.format("Something went wrong when trying to find a VisitId for %s.", domainName))
            );

            // Add the finalUrl to the map with UUID as key.
            urlMap.put(id, finalUrl);

            // Get the Status Booleans by visitId
            statusBools.add(crawlCompController.getCrawlComponentStatus(id));
        }

        // Create PageDTO to return.
        PageDTO pageDTO = new PageDTO();
        pageDTO.setAmountOfPages(dispatcherPage.getTotalPages());
        pageDTO.setHasNext(dispatcherPage.hasNext());
        pageDTO.setHasPrevious(dispatcherPage.hasPrevious());

        // Create a list of SearchDTO's to add to PageDTO.
        // SearchDTO's contain: VisitId, StatusBooleans (CrawlComponentStatus), FinalUrl.
        List<SearchDTO> dtoList = new ArrayList<>();
        for (UUID vId: visitIds) {

            // Create DTO to add to dtoList.
            SearchDTO dto = new SearchDTO();
            dto.setVisitId(vId);
            dto.setFinalUrl(urlMap.get(vId));
            dto.setRequestTimeStamp(dispatcherPage
                                    .stream()
                                    .filter(e -> e.getVisitId().equals(vId))
                                    .findFirst().get()
                                    .getRequestTimestamp());

            dto.setCrawlStatus(statusBools
                                .stream()
                                .filter(crawl -> crawl.getVisit_id().equals(vId))
                                .findFirst().get());

            dtoList.add(dto);
        }

        // Add list of SearchDTO's to PageDTO.
        pageDTO.setDtos(dtoList);

        logger.debug("Returning PageDTO containing a list of SearchDTO's.");

        return pageDTO;
    }

}
