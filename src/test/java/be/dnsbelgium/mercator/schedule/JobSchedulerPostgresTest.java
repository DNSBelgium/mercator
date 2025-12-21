package be.dnsbelgium.mercator.schedule;

import be.dnsbelgium.mercator.SimpleJobRunner;
import be.dnsbelgium.mercator.batch.BatchConfig;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;


@SpringBootTest()
@ActiveProfiles("local")
class JobSchedulerPostgresTest {

  @Autowired SimpleJobRunner simpleJobRunner;
  @Autowired BatchConfig batchConfig;

  private static final Logger logger = LoggerFactory.getLogger(JobSchedulerPostgresTest.class);

  @Test
  public void startBatch() {
    logger.info("simpleJobRunner = {}", simpleJobRunner);
    JobSchedulerPostgres scheduler = new JobSchedulerPostgres(simpleJobRunner, batchConfig,100);
    scheduler.startBatch();
  }

}