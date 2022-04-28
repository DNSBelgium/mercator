package be.dnsbelgium.mercator.api.dns;

import javassist.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

@BasePathAwareController
public class DnsCrawlController {
    private final Logger logger = LoggerFactory.getLogger(DnsCrawlController.class);

    private final DnsRepoService dnsRepoService;

    @Autowired
    public DnsCrawlController(DnsRepoService dnsRepoService) {
        this.dnsRepoService = dnsRepoService;
    }

    @GetMapping("/dns-crawler")
    public ResponseEntity<?> getInfoByVisitId(@RequestParam(name = "visitId") UUID visitId) {
        logger.debug("Requesting dns-crawler data for visitId: {}", visitId);

        try {
            return ResponseEntity.status(HttpStatus.OK).body(dnsRepoService.getInfoByVisitId(visitId));

        } catch (NotFoundException ex) {
            logger.error(ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Not found message."); // TODO: Decide on a correct error message.
        }
    }

}
