package be.dnsbelgium.mercator.batch;

import be.dnsbelgium.mercator.MercatorApplication;
import be.dnsbelgium.mercator.common.VisitRequest;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.repository.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.repository.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.repository.JobRestartException;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BatchTest {

  private static final Logger logger = LoggerFactory.getLogger(BatchTest.class);

  @Test
  public void start() throws JobInstanceAlreadyCompleteException, JobExecutionAlreadyRunningException, JobParametersInvalidException, JobRestartException {


    ApplicationContext context = new AnnotationConfigApplicationContext(MercatorApplication.class);
    //ApplicationContext context = new AnnotationConfigApplicationContext("be.dnsbelgium.mercator");

    //SpringApplication application = new SpringApplication(JobConfiguration.class, MercatorApplication.class);


    logger.info("context = {}", context);


    JobLauncher jobLauncher = context.getBean(JobLauncher.class);
    Job job = context.getBean(Job.class);
    JobParameters jobParameters = new JobParametersBuilder()
            .addString("inputFile", "test-data/delimited.csv")
            .addString("outputFile", "file:./target/test-outputs/delimitedOutput.csv")
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


  }



}
