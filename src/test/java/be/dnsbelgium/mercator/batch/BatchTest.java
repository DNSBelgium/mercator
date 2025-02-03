package be.dnsbelgium.mercator.batch;

import be.dnsbelgium.mercator.MercatorApplication;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BatchTest {

  private static final Logger logger = LoggerFactory.getLogger(BatchTest.class);

  @Test
  public void webJob() throws JobExecutionException {
    runJob("webJob");
  }

  @Test
  public void smtpJob() throws JobExecutionException {
    runJob("smtpJob");
  }

  @Test
  public void tlsJob() throws JobExecutionException {
    runJob("tlsJob");
  }

  public void runJob(String name) throws JobExecutionException {
    ApplicationContext context = new AnnotationConfigApplicationContext(MercatorApplication.class);
    JobLauncher jobLauncher = context.getBean(JobLauncher.class);
    Job job = context.getBean(name, Job.class);
    String outputFileName = "file:./target/test-outputs/" + name + ".json";

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
