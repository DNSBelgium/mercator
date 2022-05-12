package be.dnsbelgium.mercator.api.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@BasePathAwareController
public class ClusterController {
    private final Logger logger = LoggerFactory.getLogger(ClusterController.class);

    private final ClusterService clusterService;

    @Autowired
    public ClusterController(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    @GetMapping("/cluster")
    public ResponseEntity<?> getDataForVisitIdCluster(@RequestParam("visitIds") String visitIds) {
        logger.debug("getDataForVisitIdCluster was called for: {}", visitIds);

        return ResponseEntity.status(HttpStatus.OK).body(clusterService.getClusterData(visitIds));
    }
}
