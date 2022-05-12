package be.dnsbelgium.mercator.api.cluster;

import be.dnsbelgium.mercator.api.search.SearchDTO;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResult;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResultRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class ClusterService {
    private final Logger logger = LoggerFactory.getLogger(ClusterService.class);

    private final ContentCrawlResultRepository contentCrawlResultRepository;

    @Autowired
    public ClusterService(ContentCrawlResultRepository contentCrawlResultRepository) {
        this.contentCrawlResultRepository = contentCrawlResultRepository;
    }

    public List<SearchDTO> getClusterData(String visitIds) {
        logger.info("Received: {}", visitIds);

        String[] individualIds = visitIds.split(" , | ,|, |,| |(?<=\\G.{" + 36 + "})");

        List<SearchDTO> searchDTOList = new ArrayList<>();
        for (String visitId : individualIds) {
            System.out.println(visitId);

            SearchDTO dto = new SearchDTO();
            dto.setVisitId(UUID.fromString(visitId));

            List<ContentCrawlResult> contentResults = contentCrawlResultRepository.findByVisitId(UUID.fromString(visitId));
            if (!contentResults.isEmpty()) {
                Optional<ContentCrawlResult> resultWithKey = contentResults.stream().filter(r -> r.getScreenshotKey() != null).findFirst();

                resultWithKey.ifPresent(result -> {
                    dto.setDomainName(result.getDomainName());
                    dto.setScreenshotKey(result.getScreenshotKey());
                });
            }

            searchDTOList.add(dto);
        }

        logger.info("Returning list of SearchDTO's");
        return searchDTOList;
    }
}
