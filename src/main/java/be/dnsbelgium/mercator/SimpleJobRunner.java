package be.dnsbelgium.mercator;

import be.dnsbelgium.mercator.batch.BatchConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.*;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
public class SimpleJobRunner {

  // TODO: combine this class with JobRunner

  private final BatchConfig batchConfig;
  private final JobLauncher jobLauncher;
  private final Map<String, Job> jobs;
  private static final Logger logger = LoggerFactory.getLogger(SimpleJobRunner.class);

  // for some reason IntelliJ does not find the JobLauncher bean
  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  public SimpleJobRunner(BatchConfig batchConfig, JobLauncher jobLauncher, Map<String, Job> jobs) {
    this.batchConfig = batchConfig;
    this.jobLauncher = jobLauncher;
    this.jobs = jobs;
  }

  public void run() throws Exception {
    logger.info("SimpleJobRunner.run() started");
    logger.info("we will run these jobs: {}", jobs.keySet());

    jobs.forEach((name, job) -> {
      try {
        // TODO: do we really need these strings as JobParameters ?
        // why not simply inject them into the Spring components that need them?
        String inputFileParam = "file://" + batchConfig.getInputFile();
        String outputFileParam = "file://" + batchConfig.outputPathFor(job.getName()).toAbsolutePath();

        JobParameters params = new JobParametersBuilder()
                .addString("inputFile", inputFileParam)
                .addString("outputFile", outputFileParam)
                .addString("job_uuid", UUID.randomUUID().toString())
                .toJobParameters();

        logger.info("Starting job {} with these parameters: {}", name, params);
        jobLauncher.run(job, params);

      } catch (Exception e) {
        logger.atError()
                .setMessage( "Failed to run job: {}")
                .addArgument(name)
                .setCause(e)
                .log();
      }
    });
    logger.info("All batch jobs executed");
  }


}
