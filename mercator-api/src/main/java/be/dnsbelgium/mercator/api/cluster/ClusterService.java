package be.dnsbelgium.mercator.api.cluster;

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
        for (String receivedVisitId : visitIds) {
            if (receivedVisitId.equals("")) continue; // Last check in case the frontend missed one.

            ClusterDTO dto = new ClusterDTO();
            dto.setReceivedVisitId(receivedVisitId);

            UUID visitId;
            try {
                visitId = UUID.fromString(receivedVisitId);
            } catch (IllegalArgumentException ex) {
                logger.debug("VisitId {} wasn't a valid UUID.", receivedVisitId);
                clusterDTOList.add(dto);
                continue;
            }

            dto.setVisitId(visitId);
            List<ContentCrawlResult> contentResults = contentCrawlResultRepository.findByVisitId(visitId);

            contentResults.stream().filter(r -> r.getScreenshotKey() != null).findFirst().ifPresent(result -> {
                dto.setDomainName(result.getDomainName());
                dto.setScreenshotKey(result.getScreenshotKey());
            });

            clusterDTOList.add(dto);
        }

        return clusterDTOList;
    }
}
