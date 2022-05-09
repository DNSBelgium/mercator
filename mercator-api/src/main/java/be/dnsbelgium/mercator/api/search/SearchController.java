package be.dnsbelgium.mercator.api.search;

import be.dnsbelgium.mercator.content.persistence.ContentCrawlResultRepository;
import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@BasePathAwareController
public class SearchController {
    private final Logger logger = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;
    private final ContentCrawlResultRepository contentCrawlResultRepository;

    @Autowired
    public SearchController(SearchService searchService, ContentCrawlResultRepository contentCrawlResultRepository) {
        this.searchService = searchService;
        this.contentCrawlResultRepository = contentCrawlResultRepository;
    }

    /**
     * Look at SearchService -> getInfoForDomain for more info.
     * @param domainName Domain the frontend user is searching for.
     * @return Either: HTTPStatus 200 with a List of SearchDTO's OR HTTPStatus 404 with a message.
     */
    @GetMapping("/find-visits/{domainName}") // Example: find-visits/abc.be/?page=1
    public ResponseEntity<?> getPageForDomainByName(@PathVariable String domainName, @RequestParam(name = "page") int pageNumber) {
        logger.debug(String.format("GET was called for: %s. At page: %d.", domainName, pageNumber));

        try {
            return ResponseEntity.status(HttpStatus.OK).body(searchService.getPageForDomain(domainName, pageNumber));
        } catch (NotFoundException ex) {
            logger.debug(ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        } catch (ExecutionException | InterruptedException ex) {
            logger.error("Something wrong happened: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("");
        }
    }

    @GetMapping("/findScreenshotsByVisitIds")
    public ResponseEntity<?> getTest(@RequestParam("visitIdList") List<UUID> visitIdList) {
        logger.debug("Getting Screenshot keys.");

        try {
            logger.debug("Returning keys.");
            List<String> stuffToReturn = contentCrawlResultRepository.findScreenshotsByVisitIds(visitIdList);
            return ResponseEntity.status(HttpStatus.OK).body(stuffToReturn);
        } catch(Exception ex) {
            logger.error("Something wrong happened: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("");
        }
    }
}
