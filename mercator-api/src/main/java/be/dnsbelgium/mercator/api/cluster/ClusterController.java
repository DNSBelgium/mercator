package be.dnsbelgium.mercator.api.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@BasePathAwareController
public class ClusterController {
    private final Logger logger = LoggerFactory.getLogger(ClusterController.class);

    private final ClusterService clusterService;

    @Autowired
    public ClusterController(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    @PostMapping("/cluster") // This is a POST mapping to enable more than 200 VisitId's.
    public ResponseEntity<?> getDataForVisitIdCluster(@RequestBody ClusterDTO clusterDTO) {
        logger.debug("getDataForVisitIdCluster was called for: {}", clusterDTO.getData());

        return ResponseEntity.status(HttpStatus.OK).body(clusterService.getClusterData(clusterDTO.getData()));
    }
}
