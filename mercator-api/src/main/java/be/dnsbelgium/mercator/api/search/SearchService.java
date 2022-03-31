package be.dnsbelgium.mercator.api.search;

import be.dnsbelgium.mercator.api.status.CrawlComponentStatus;
import be.dnsbelgium.mercator.api.status.CrawlComponentStatusController;
import be.dnsbelgium.mercator.api.status.CrawlComponentStatusService;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResultRepository;
import be.dnsbelgium.mercator.dispatcher.persistence.DispatcherEvent;
import be.dnsbelgium.mercator.dispatcher.persistence.DispatcherEventRepository;
import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeaturesRepository;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

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
     * Gets the all the necessary information needed for the frontend.
     * Frontend calls a search by domain name, gets a list of SearchDTO's as the return.
     * @param domainName Domain the frontend is searching for. F.ex.: abc.be
     * @return List of SearchDTO's containing VisitId, FinalURL and ContentCrawler booleans.
     * @throws NotFoundException When a requested domain is not found in the database.
     */
    public List<SearchDTO> getInfoForDomain(String domainName) throws NotFoundException {
        logger.info("Searching for " + domainName);

        List<DispatcherEvent> dispatcherEvents = dispatchRepo.findDispatcherEventByDomainName(domainName);

        if (dispatcherEvents.size() == 0) {
            throw new NotFoundException(String.format("Domain %s was not yet crawled or does not exist.", domainName));
        }

        // Get list of visitId's for DomainName
        List<UUID> visitIds = new ArrayList<>();
        for (DispatcherEvent event: dispatcherEvents) {
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

        // Create a list of SearchDTO's to return.
        // DTO's contain: VisitId, StatusBooleans (CrawlComponentStatus), FinalUrl.
        List<SearchDTO> listToReturn = new ArrayList<>();
        for (UUID vId: visitIds) {

            // Create DTO to add to listToReturn.
            SearchDTO dto = new SearchDTO();
            dto.setVisitId(vId);
            dto.setFinalUrl(urlMap.get(vId));
            dto.setRequestTimeStamp(dispatcherEvents
                                    .stream()
                                    .filter(e -> e.getVisitId().equals(vId))
                                    .findFirst().get()
                                    .getRequestTimestamp());

            dto.setCrawlStatus(statusBools
                                .stream()
                                .filter(crawl -> crawl.getVisit_id().equals(vId))
                                .findFirst().get());

            listToReturn.add(dto);
        }

        // Return list sorted by requestTimeStamp
        return listToReturn
                .stream()
                .sorted(Comparator.comparing(SearchDTO::getRequestTimeStamp))
                .collect(Collectors.toList());
    }

}
