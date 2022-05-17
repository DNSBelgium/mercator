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
     * ClusterDTO's can be returned with the following values: (or nulls)
     *      Received VisitId -> one of the VisitId's given from the parameter.
     *      Domain Name, screenshotKey, 'confirmed' visitId.
     *      A 'confirmed visitId' means a visitId that matches with an existing database UUID.
     * @param visitIds List of VisitId's in String representation.
     * @return list of ClusterDTO's.
     */
    public List<ClusterDTO> getClusterData(List<String> visitIds) {
        logger.info("Received a list of visitId's. Size: {}", visitIds.size());

        List<ClusterDTO> clusterDTOList = new ArrayList<>();
        for (String visitId : visitIds) {
            if (visitId.equals("")) continue; // Last check in case the frontend missed one.

            ClusterDTO dto = new ClusterDTO();
            dto.setReceivedVisitId(visitId);

            List<ContentCrawlResult> contentResults = new ArrayList<>();
            try {
                dto.setVisitId(UUID.fromString(visitId));
                contentResults = contentCrawlResultRepository.findByVisitId(UUID.fromString(visitId));

            } catch (IllegalArgumentException ex) {
                logger.debug("VisitId {} wasn't a valid UUID.", visitId);
                dto.setVisitId(null);
                dto.setDomainName(null);
                dto.setScreenshotKey(null);
            }

            if (!contentResults.isEmpty()) {
                Optional<ContentCrawlResult> resultWithKey = contentResults.stream().filter(r -> r.getScreenshotKey() != null).findFirst();

                resultWithKey.ifPresent(result -> {
                    dto.setDomainName(result.getDomainName());
                    dto.setScreenshotKey(result.getScreenshotKey());
                });
            }

            clusterDTOList.add(dto);
        }

        logger.info("Returning list of ClusterDTO's");
        return clusterDTOList;
    }
}
