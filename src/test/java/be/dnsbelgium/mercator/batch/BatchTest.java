package be.dnsbelgium.mercator.batch;

import be.dnsbelgium.mercator.MercatorApplication;
import be.dnsbelgium.mercator.common.VisitRequest;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
  public void start() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {

    JavaTimeModule javaTimeModule = new JavaTimeModule();
    logger.info("javaTimeModule = {}", javaTimeModule);


    ApplicationContext context = new AnnotationConfigApplicationContext(MercatorApplication.class);
    logger.info("context = {}", context);

    JobLauncher jobLauncher = context.getBean(JobLauncher.class);
    Job job = context.getBean(Job.class);

    logger.info("job = {}", job);
    logger.info("job = {}", job.getName());


    JobParameters jobParameters = new JobParametersBuilder()
            .addString("inputFile", "test-data/visit_requests.csv")
            .addString("outputFile", "file:./target/test-outputs/web_crawl_results.json")
            .addString("job_uuid", UUID.randomUUID().toString())
            .toJobParameters();

    // when
    JobExecution jobExecution = jobLauncher.run(job, jobParameters);

    logger.info("jobExecution = {}", jobExecution);
    logger.info("jobExecution = {}", jobExecution.getExitStatus());
    logger.info("jobExecution = {}", jobExecution.getLastUpdated());

    for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
      logger.info("stepExecution = {}", stepExecution);
    }

    assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());

    VisitRequest request = new VisitRequest("uuid", "dnsbelgium.be");
    logger.info("request = {}", request);
  }

  @Test
  public void creditJob() throws JobExecutionException {
    runJob(CreditJobConfiguration.class);
  }

  @Test
  public void webJob() throws JobExecutionException {
    runJob(WebJobConfig.class);
  }

  public void runJob(Class<?> clazz) throws JobExecutionException {
    ApplicationContext context = new AnnotationConfigApplicationContext(clazz);
    JobLauncher jobLauncher = context.getBean(JobLauncher.class);
    Job job = context.getBean(Job.class);
    JobParameters jobParameters = new JobParametersBuilder()
            .addString("inputFile", "test-data/delimited.csv")
            .addString("outputFile", "file:./target/test-outputs/output.csv")
            .toJobParameters();
    JobExecution jobExecution = jobLauncher.run(job, jobParameters);
    logger.info("jobExecution = {}", jobExecution);
    logger.info("jobExecution = {}", jobExecution.getExitStatus());
    logger.info("jobExecution = {}", jobExecution.getLastUpdated());
    for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
      logger.info("stepExecution = {}", stepExecution);
    }
    assertEquals(BatchStatus.COMPLETED, jobExecution.getStatus());
  }


}
