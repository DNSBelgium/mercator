package be.dnsbelgium.mercator.api.cluster;

import be.dnsbelgium.mercator.api.search.SearchDTO;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResult;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ClusterService {
    private final Logger logger = LoggerFactory.getLogger(ClusterService.class);

    private final ContentCrawlResultRepository contentCrawlResultRepository;

    @Autowired
    public ClusterService(ContentCrawlResultRepository contentCrawlResultRepository) {
        this.contentCrawlResultRepository = contentCrawlResultRepository;
    }

    public void foo (String visitIds) {
        logger.info("Received: {}", visitIds);

        String[] individualIds = visitIds.split(", |,| ");

        List<String> screenshotKeys = new ArrayList<>();
        for (String visitId : individualIds) {

            List<ContentCrawlResult> contentResults = contentCrawlResultRepository.findByVisitId(UUID.fromString(visitId));
            if (!contentResults.isEmpty()) {
                Optional<ContentCrawlResult> resultWithKey = contentResults.stream().filter(r -> r.getScreenshotKey() != null).findFirst();
                resultWithKey.ifPresent(contentCrawlResult -> screenshotKeys.add(contentCrawlResult.getScreenshotKey()));
            }
        }

    }
}
