package be.dnsbelgium.mercator.batch;

import be.dnsbelgium.mercator.MercatorApplication;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
public class BatchTest {

  private static final Logger logger = LoggerFactory.getLogger(BatchTest.class);

  @Autowired @Qualifier("webJob") Job webJob;
  @Autowired @Qualifier("tlsJob") Job tlsJob;
  @Autowired @Qualifier("smtpJob") Job smtpJob;

  @Test
  public void webJob() throws JobExecutionException {
    run(webJob);
  }

  @Test
  public void smtpJob() throws JobExecutionException {
    run(smtpJob);
  }

  @Test
  public void tlsJob() throws JobExecutionException {
    run(tlsJob);
  }

  public void run(Job job) throws JobExecutionException {
    ApplicationContext context = new AnnotationConfigApplicationContext(MercatorApplication.class);
    JobLauncher jobLauncher = context.getBean(JobLauncher.class);
    String outputFileName = "file:./target/test-outputs/" + job.getName() + ".json";

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


}
