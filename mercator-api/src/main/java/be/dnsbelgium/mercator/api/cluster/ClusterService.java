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

    /**
     * Gathers data necessary for a Cluster View.
     * SearchDTO's can be returned with the following values: (or nulls)
     *      Received VisitId -> one of the VisitId's given from the parameter.
     *      Domain Name, screenshotKey, 'confirmed' visitId.
     *      A 'confirmed visitId' means a visitId that matches with an existing database UUID.
     * @param visitIds Single large String of VisitId's.
     * @return list of SearchDTO's.
     */
    public List<SearchDTO> getClusterData(String visitIds) {
        logger.info("Received a String of visitId's");

        // The frontend sends NEWLINE when a visitId was split by \n.
        // I could not find a UTF-8 / 16 way to fix that, so I used string.replace to set it to "NEWLINE".
        String[] individualIds = visitIds.split(" , | ,|, |,| |NEWLINE");

        List<SearchDTO> searchDTOList = new ArrayList<>();
        for (String visitId : individualIds) {
            if (visitId.equals("")) continue;

            SearchDTO dto = new SearchDTO();
            dto.setReceivedVisitId(visitId);

            try {
                dto.setVisitId(UUID.fromString(visitId));

                List<ContentCrawlResult> contentResults = contentCrawlResultRepository.findByVisitId(UUID.fromString(visitId));
                if (!contentResults.isEmpty()) {
                    Optional<ContentCrawlResult> resultWithKey = contentResults.stream().filter(r -> r.getScreenshotKey() != null).findFirst();

                    resultWithKey.ifPresent(result -> {
                        dto.setDomainName(result.getDomainName());
                        dto.setScreenshotKey(result.getScreenshotKey());
                    });
                }

            // When a given visitId isn't a valid UUID
            } catch (IllegalArgumentException ex) {
                logger.debug("VisitId {} wasn't a valid UUID.", visitId);
                dto.setVisitId(null);
                dto.setDomainName(null);
                dto.setScreenshotKey(null);
            }

            searchDTOList.add(dto);
        }

        logger.info("Returning list of SearchDTO's");
        return searchDTOList;
    }
}
