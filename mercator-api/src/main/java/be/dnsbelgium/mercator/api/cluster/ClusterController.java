package be.dnsbelgium.mercator.api.cluster;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.rest.webmvc.BasePathAwareController;
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

    @PostMapping("/cluster")
    public void foo(@RequestBody TempDTO tempDTO) {
        tempDTO.setSending(
                "dba96cca-44a8-4ddc-8969-4aec2256f008, 313441b4-25f7-42b0-980c-1ced48978b18,6670d37a-2b8b-4c25-bd8f-4528136d6337 2a01e6c5-f3b0-4373-818c-24f8863af99f"
        );
        logger.debug(tempDTO.toString());
        clusterService.foo(tempDTO.getSending());
    }
}
