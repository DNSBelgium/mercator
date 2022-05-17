package be.dnsbelgium.mercator.api.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@BasePathAwareController
public class ClusterController {
    private final Logger logger = LoggerFactory.getLogger(ClusterController.class);

    private final ClusterService clusterService;

    @Autowired
    public ClusterController(ClusterService clusterService) {
        this.clusterService = clusterService;
    }

    /**
     * Controller function to receive a List of UUID's in String representation.
     * The reason this is a POST mapping and not a GET mapping is because
     * a query param, HTTP header or cookie wouldn't be able to handle so many visitId's.
     * @param visitIds simple object to contain the incoming String value.
     * @return a List of SearchDTO's.
     */
    @PostMapping("/cluster")
    public ResponseEntity<?> getClusterData(@RequestBody List<String> visitIds) {
        logger.debug("getClusterData was called.");

        try {
            return ResponseEntity.status(HttpStatus.OK).body(clusterService.getClusterData(visitIds));
        } catch (Exception ex) {
            logger.warn(ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Something went wrong.");
        }
    }
}
