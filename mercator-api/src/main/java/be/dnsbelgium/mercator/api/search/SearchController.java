package be.dnsbelgium.mercator.api.search;

import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@BasePathAwareController
public class SearchController {
    private final Logger logger = LoggerFactory.getLogger(SearchController.class);

    private final SearchService searchService;

    @Autowired
    public SearchController(SearchService searchService) {
        this.searchService = searchService;
    }

    /**
     * Look at SearchService -> getInfoForDomain for more info.
     * @param domainName Domain the frontend user is searching for.
     * @return Either: HTTPStatus 200 with a List of SearchDTO's OR HTTPStatus 404 with a message.
     */
    @GetMapping("/find-visits/{domainName}") // Example: find-visits/abc.be
    public ResponseEntity<?> getInfoForDomainByName(@PathVariable String domainName) { //@RequestParam(name = "domain") String domainName
        logger.debug("getInfoForDomainByName was called for: " + domainName);

        try {
            return ResponseEntity.status(HttpStatus.OK).body(searchService.getInfoForDomain(domainName));
        } catch (NotFoundException ex) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
        }
    }
}

