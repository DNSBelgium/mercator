package be.dnsbelgium.mercator.batch;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.test.ObjectMother;
import be.dnsbelgium.mercator.vat.WebProcessor;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.PathResource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.nio.file.Path;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SuppressWarnings({"SpringJavaInjectionPointsAutowiringInspection", "SpringBootApplicationProperties"})
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE, properties = { "crawler.dns.geoIP.enabled=false" })
public class BatchTest {

  private static final Logger logger = LoggerFactory.getLogger(BatchTest.class);

  @Autowired @Qualifier("webJob") Job webJob;

  @MockitoBean
  WebProcessor webProcessor;

  @Autowired JobRepository jobRepository;
  @Autowired JobLauncher jobLauncher;
  @Autowired ObjectMapper objectMapper;

  @TempDir
  Path tempDir;

  ObjectMother objectMother = new ObjectMother();

  @Test
  public void webJob() throws Exception {
    WebCrawlResult crawlResult1 = objectMother.webCrawlResult1();
    WebCrawlResult crawlResult2 = objectMother.webCrawlResult2();
    when(webProcessor.process(any(VisitRequest.class)))
            .thenReturn(crawlResult1)
            .thenReturn(crawlResult2);
    Path outputPath = tempDir.resolve("web.json");
    run(webJob, outputPath);

    // Now check the resulting output
    JacksonJsonObjectReader<WebCrawlResult> jsonObjectMarshaller
            = new JacksonJsonObjectReader<>(objectMapper, WebCrawlResult.class);
    jsonObjectMarshaller.open(new PathResource(outputPath));
    WebCrawlResult w1 = jsonObjectMarshaller.read();
    WebCrawlResult w2 = jsonObjectMarshaller.read();

    logger.info("w1 = {}", w1);
    logger.info("w2 = {}", w2);

    assertThat(w1).isEqualTo(crawlResult1);
    assertThat(w2).isEqualTo(crawlResult2);
  }


  public void run(Job job, Path outputPath) throws JobExecutionException {
    logger.info("jobRepository = {}", jobRepository);
    logger.info("jobLauncher = {}", jobLauncher);

    JobParameters jobParameters = new JobParametersBuilder()
            .addString("inputFile", "test-data/visit_requests.csv")
            .addString("outputFile", "file:" + outputPath.toAbsolutePath())
            .addString("job_uuid", UUID.randomUUID().toString())
            .toJobParameters();
    JobExecution jobExecution = jobLauncher.run(job, jobParameters);
    logger.info("jobExecution.startTime = {}", jobExecution.getStartTime());
    logger.info("jobExecution.endTime   = {}", jobExecution.getEndTime());
    logger.info("jobExecution.exitstatus = {}", jobExecution.getExitStatus());
    logger.info("jobExecution.lastUpdated = {}", jobExecution.getLastUpdated());
    for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
      logger.info("stepExecution = {}", stepExecution);
    }
    assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
  }

}
