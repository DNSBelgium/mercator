package be.dnsbelgium.mercator;

import be.dnsbelgium.mercator.persistence.DuckDataSource;
import be.dnsbelgium.mercator.scheduling.BatchConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

@Profile("batch")
@SpringBootApplication
@ComponentScan(
    basePackages = "be.dnsbelgium.mercator",

    includeFilters = {
        @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".*JobConfig$"),
        @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = BatchConfig.class)
    }
)
public class BatchApplication implements CommandLineRunner {

  private final ApplicationContext context;
  private static final Logger logger = LoggerFactory.getLogger(BatchApplication.class);

  @Autowired
  private JobLauncher jobLauncher;

  @Autowired
  Map<String, Job> jobs;

  @Value("${in}")  // Read input filename from command line
  private String inputFile;

  @Value("${out}")  // Read input filename from command line
  private String outputPath;

  public BatchApplication(ApplicationContext context) {
    this.context = context;
  }

  public static void main(String[] args) {
    SpringApplication.run(BatchApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {

    jobs.forEach((name, job) -> {
        try {
          JobParameters params = new JobParametersBuilder()
              .addString("inputFile", "file:" + inputFile)
              .addString("outputFile", "file:" + outputPath + "/" + job.getName() + ".json")
              .addString("job_uuid", UUID.randomUUID().toString())
              .toJobParameters();
          System.out.println("Running job: " + name);
          jobLauncher.run(job, params);
        } catch (Exception e) {
          logger.error("Failed to run job: " + name, e);
        }
    });

    logger.info("Batch job executed!");
    SpringApplication.exit(context); // Zorgt dat de app stopt
  }


}
