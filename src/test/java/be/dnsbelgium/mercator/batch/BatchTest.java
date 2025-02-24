package be.dnsbelgium.mercator.batch;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.persistence.DuckDataSource;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.github.f4b6a3.ulid.Ulid;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.JsonFileItemWriter;
import org.springframework.batch.item.json.builder.JsonFileItemWriterBuilder;
import org.springframework.batch.item.support.ListItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.PathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.JdbcTransactionManager;

import java.io.File;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
//@Disabled
public class BatchTest {

  private static final Logger logger = LoggerFactory.getLogger(BatchTest.class);

  @Autowired @Qualifier("webJob") Job webJob;
//  @Autowired @Qualifier("tls") Job tlsJob;
//  @Autowired @Qualifier("smtp") Job smtpJob;
//  @Autowired @Qualifier("smtpJob") Job smtpJob;


  @Autowired JobRepository jobRepository;
  @Autowired JobLauncher jobLauncher;
  @Autowired JdbcTransactionManager transactionManager;

  @Test
  public void webJob() throws JobExecutionException {
    run(webJob);
  }

//  @Test
//  public void smtpJob() throws JobExecutionException {
//    run(smtpJob);
//  }
//
//  @Test
//  public void tlsJob() throws JobExecutionException {
//    run(tlsJob);
//  }

  public void run(Job job) throws JobExecutionException {
    String outputFileName = "file:./target/test-outputs/" + job.getName() + ".json";
    logger.info("jobRepository = {}", jobRepository);
    logger.info("jobLauncher = {}", jobLauncher);

    JobParameters jobParameters = new JobParametersBuilder()
            .addString("inputFile", "test-data/visit_requests.csv")
            .addString("outputFile", outputFileName)
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

  @Test
  public void simpleJob() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {
    logger.info("creating simpleJob");
    List<VisitRequest> requestList = new ArrayList<>();
    for (int i = 0; i < 100; i++) {
      requestList.add(new VisitRequest(Ulid.fast().toString(), "abc-" + i +".be"));
    }
    ItemReader<VisitRequest> reader = new ListItemReader<>(requestList);

    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    JacksonJsonObjectMarshaller<WebCrawlResult> jsonObjectMarshaller
            = new JacksonJsonObjectMarshaller<>(objectMapper);

    PathResource output = new PathResource("output.json");

    JsonFileItemWriter<WebCrawlResult> itemWriter = new JsonFileItemWriterBuilder<WebCrawlResult>()
            .name("WebCrawlResultWriter")
            .jsonObjectMarshaller(jsonObjectMarshaller)
            .resource(output)
            .build();

    Step step = new StepBuilder("web", jobRepository)
            .<VisitRequest, WebCrawlResult>chunk(10, transactionManager)
            .reader(reader)
            .processor(new MyProcessor())
            .writer(itemWriter)
            .build();

    Job job = new JobBuilder("web", jobRepository)
            .start(step)
            .listener(new ParquetMaker(output))
            .build();

    JobParameters jobParameters = new JobParametersBuilder()
            .addString("job_uuid", UUID.randomUUID().toString())
            .toJobParameters();
    JobExecution jobExecution = jobLauncher.run(job, jobParameters);
    System.out.println("jobExecution = " + jobExecution);


  }

  private static class MyProcessor implements ItemProcessor<VisitRequest, WebCrawlResult> {

    @Override
    public WebCrawlResult process(VisitRequest item) throws Exception {
      logger.info("Processing item = {}", item);
      return WebCrawlResult.builder()
              .crawlStarted(Instant.now())
              .crawlFinished(Instant.now().plusMillis(150))
              .visitId(item.getVisitId())
              .domainName(item.getDomainName())
              .startUrl("www.example.com")
              .build();
    }
  }

  private static class ParquetMaker implements JobExecutionListener {

    private final PathResource inputFile;

    private ParquetMaker(PathResource inputFile) {
      this.inputFile = inputFile;
    }

    //new PathResource("output.json")

    @SneakyThrows
    @Override
    public void afterJob(JobExecution jobExecution) {
      logger.info("jobExecution = {}", jobExecution);
      logger.info("jobExecution.getExecutionContext() = {}", jobExecution.getExecutionContext());

      if (jobExecution. getStatus() == BatchStatus.COMPLETED) {
        JdbcClient client = JdbcClient.create(DuckDataSource.memory());
        String copy = String.format("copy (select * from '%s') to 'web-output.parquet'", inputFile.getFile().getAbsolutePath());
        logger.info("copy stmt = {}", copy);
        client.sql(copy).update();
        logger.info("parquet file created");
      }

    }
  }

}
