package be.dnsbelgium.mercator.scheduling;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.UUID;

import static org.springframework.scheduling.annotation.Scheduled.CRON_DISABLED;

@SuppressWarnings("SqlDialectInspection")
@Component
@EnableScheduling
public class Scheduler {

  @Value("${smtp.enabled:true}")
  private boolean smtpEnabled;

  private final JobLauncher jobLauncher;
  private final Job webJob;
  private final Job tlsJob;
  private final Job smtpJob;

  private static final Logger logger = LoggerFactory.getLogger(Scheduler.class);

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Autowired
  public Scheduler(JobLauncher jobLauncher,
                   @Qualifier("webJob") Job webJob,
                   @Qualifier("tlsJob") Job tlsJob,
                   @Qualifier("smtpJob") Job smtpJob
  ) {
    this.jobLauncher = jobLauncher;
    this.webJob = webJob;
    this.tlsJob = tlsJob;
    this.smtpJob = smtpJob;
  }

  // todo: re-enable
  @Scheduled(cron = CRON_DISABLED)
  public void web() throws JobExecutionException {
    logger.info("Scheduling web job");
    // todo: pass correct parameters
    // now it fails when the csv file is not found
    JobParameters jobParameters = new JobParametersBuilder()
            .addString("inputFile", "file:test-data/visit_requests.csv")
            .addString("outputFile", "file:webcrawlresult.json")
            .addString("job_uuid", UUID.randomUUID().toString())
            .toJobParameters();

    jobLauncher.run(webJob, jobParameters);
  }

  public void tls() throws JobExecutionException {
    // todo: pass correct parameters
    // now it fails when the csv file is not found
    JobParameters jobParameters = new JobParametersBuilder()
            .addString("inputFile", "file:test-data/visit_requests.csv")
            .addString("outputFile", "file:tlscrawlresult.json")
            .addString("job_uuid", UUID.randomUUID().toString())
            .toJobParameters();

    jobLauncher.run(tlsJob, jobParameters);
  }

  public void smtp() throws JobExecutionException {
    if (!smtpEnabled) {
      logger.info("smtp disabled");
      return;
    }
    // todo: pass correct parameters
    // now it fails when the csv file is not found
    JobParameters jobParameters = new JobParametersBuilder()
            .addString("inputFile", "file:test-data/visit_requests.csv")
            .addString("outputFile", "file:smtp.json")
            .addString("job_uuid", UUID.randomUUID().toString())
            .toJobParameters();

    jobLauncher.run(smtpJob, jobParameters);
  }

}
