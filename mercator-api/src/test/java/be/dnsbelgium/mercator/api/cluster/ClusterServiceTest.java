package be.dnsbelgium.mercator.api.cluster;

import be.dnsbelgium.mercator.content.persistence.ContentCrawlResult;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResultRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringJUnitConfig(ClusterService.class)
class ClusterServiceTest {

    @MockBean
    ContentCrawlResultRepository contentCrawlResultRepository;

    @Autowired
    ClusterService clusterService;

    @Test
    void getClusterData() {
        ContentCrawlResult contentCrawlResult1 = new ContentCrawlResult();
        contentCrawlResult1.setScreenshotKey("foo");

        ContentCrawlResult contentCrawlResult2 = new ContentCrawlResult();
        contentCrawlResult2.setScreenshotKey("bar");
        ContentCrawlResult contentCrawlResult3 = new ContentCrawlResult();
        contentCrawlResult3.setScreenshotKey("foobar");

        when(contentCrawlResultRepository.findByVisitId(any(UUID.class))).thenReturn(
                Collections.singletonList(contentCrawlResult1),
                Arrays.asList(contentCrawlResult2, contentCrawlResult3),
                Collections.emptyList()
        );

        List<String> visitIds = Arrays.asList(UUID.randomUUID().toString(), UUID.randomUUID().toString(), UUID.randomUUID().toString());
        List<ClusterDTO> clusterData = clusterService.getClusterData(visitIds);

        assertThat(clusterData).hasSize(3);
        assertThat(clusterData.get(0).getScreenshotKey()).isEqualTo("foo");
        assertThat(clusterData.get(1).getScreenshotKey()).isEqualTo("bar");
        assertThat(clusterData.get(2).getScreenshotKey()).isNull();
    }

    @Test
    void getClusterDataWrongVisitId() {
        List<ClusterDTO> clusterData = clusterService.getClusterData(List.of("wrong visit id"));

        assertThat(clusterData).hasSize(1);
        assertThat(clusterData.get(0).getReceivedVisitId()).isEqualTo("wrong visit id");
        assertThat(clusterData.get(0).getVisitId()).isNull();
        assertThat(clusterData.get(0).getDomainName()).isNull();
        assertThat(clusterData.get(0).getScreenshotKey()).isNull();
    }

}