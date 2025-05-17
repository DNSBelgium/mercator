package be.dnsbelgium.mercator.batch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.lang.NonNull;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class JobListener implements JobExecutionListener {

  private static final Logger logger = LoggerFactory.getLogger(JobListener.class);
  private final CountDownLatch latch = new CountDownLatch(1);
  private final String jobName;

  public JobListener(String jobName) {
    this.jobName = jobName;
  }

  @Override
  public void beforeJob(@NonNull JobExecution jobExecution) {
    logger.info("beforeJob: jobName={}, instance={}", jobName, jobExecution.getJobInstance());
  }

  @Override
  public void afterJob(@NonNull JobExecution jobExecution) {
    logger.info("afterJob: jobName={}, instance={}", jobName, jobExecution.getJobInstance());

    jobExecution.getAllFailureExceptions().forEach(e -> logger.error("Failure while executing job {}", jobName, e));

    jobExecution.getStepExecutions().forEach(step -> {
      step.getFailureExceptions().forEach(e -> {
        logger.error("Failure(s) in step {} : {}", step.getStepName(), e.getMessage(), e);
      });

    });

    latch.countDown();
  }

  public void await() {
    logger.info("Awaiting job {} ...", jobName);
    try {
      long start = System.currentTimeMillis();
      while (!latch.await(20, TimeUnit.SECONDS)) {
        long seconds = (System.currentTimeMillis() - start)/1000;
        logger.info("Awaiting job {} for already {} seconds ...", jobName, seconds);
      }
      logger.info("Job {} completed", jobName);
    } catch (InterruptedException e) {
      logger.warn("Interrupted while waiting for job {} latch: {}", jobName, e.getMessage());
      Thread.currentThread().interrupt();
    }
  }

}
