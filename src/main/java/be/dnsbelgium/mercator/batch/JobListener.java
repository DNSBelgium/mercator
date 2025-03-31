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

  @Override
  public void beforeJob(@NonNull JobExecution jobExecution) {
    logger.info("beforeJob: {}", jobExecution.getJobInstance());
  }

  @Override
  public void afterJob(@NonNull JobExecution jobExecution) {
    logger.info("afterJob: {}", jobExecution.getJobInstance());
    latch.countDown();
  }

  public void await() {
    logger.info("Awaiting job ...");
    try {
      long start = System.currentTimeMillis();
      while (!latch.await(20, TimeUnit.SECONDS)) {
        long seconds = (System.currentTimeMillis() - start)/1000;
        logger.info("Awaiting latch for {} seconds ...", seconds);
      }
      logger.info("Job completed");
    } catch (InterruptedException e) {
      logger.warn("Interrupted while waiting for latch: {}", e.getMessage());
      Thread.currentThread().interrupt();
    }
  }

}
