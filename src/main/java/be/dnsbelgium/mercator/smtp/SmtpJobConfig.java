package be.dnsbelgium.mercator.smtp;

import be.dnsbelgium.mercator.batch.BatchConfig;
import be.dnsbelgium.mercator.batch.DelegatingItemProcessor;
import be.dnsbelgium.mercator.batch.JsonItemWriter;
import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.persistence.SmtpRepository;
import be.dnsbelgium.mercator.smtp.dto.SmtpVisit;
import tools.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.task.AsyncTaskExecutor;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.nio.file.Path;

@SuppressWarnings("SpringElInspection")
@Configuration
public class SmtpJobConfig {

  private static final Logger logger = LoggerFactory.getLogger(SmtpJobConfig.class);
  private static final String JOB_NAME = "smtp";

  @Value("${smtp.corePoolSize:1000}")
  private int corePoolSize;

  @Value("${smtp.maxPoolSize:1000}")
  private int maxPoolSize;

  @Value("${smtp.chunkSize:1000}")
  private int chunkSize;

  @Bean
  @StepScope
  public FlatFileItemReader<VisitRequest> smtpItemReader(@Value("#{jobParameters[inputFile]}") Resource resource) {
    logger.info("creating FlatFileItemReader for resource {}", resource);
    return new FlatFileItemReaderBuilder<VisitRequest>()
            .name("itemReader")
            .resource(resource)
            .delimited()
            .names("visitId", "domainName")
            .targetType(VisitRequest.class)
            .build();
  }

  @Bean
  @StepScope
  public JsonItemWriter<SmtpVisit> smtpItemWriter(
          MeterRegistry meterRegistry,
          BatchConfig batchConfig, SmtpRepository repository, ObjectMapper objectMapper) {
    Path outputDirectory = batchConfig.outputDirectoryFor(JOB_NAME);
    return new JsonItemWriter<>(meterRegistry, repository, objectMapper, outputDirectory, SmtpVisit.class);
  }

  @Bean
  @Qualifier(JOB_NAME)
  public AsyncTaskExecutor smtpTaskExecutor(SmtpCrawlerConfiguration configuration) {
    if (configuration.virtualThreads) {
      logger.info("using a VirtualThreadTaskExecutor");
      return new VirtualThreadTaskExecutor(JOB_NAME + "-virtual");
    }
    var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(-1);
    executor.setThreadNamePrefix(JOB_NAME);
    logger.info("SMTP: executor corePoolSize={} maxPoolSize={}", corePoolSize, maxPoolSize);
    return executor;
  }

  @Bean(name = "smtpJob")
  @ConditionalOnProperty(name = "job.smtp.enabled", havingValue = "true")
  public Job smtpJob(JobRepository jobRepository,
                     ItemReader<VisitRequest> smtpItemReader,
                     SmtpCrawler smtpCrawler,
                     JsonItemWriter<SmtpVisit> itemWriter,
                     @Qualifier(JOB_NAME) AsyncTaskExecutor taskExecutor) {

    logger.info("creating smtpJob with JOB_NAME={}", JOB_NAME);
    DelegatingItemProcessor<SmtpVisit> itemProcessor = new DelegatingItemProcessor<>(smtpCrawler);

    Step step = new StepBuilder(JOB_NAME, jobRepository)
            .<VisitRequest, SmtpVisit>chunk(chunkSize)
            .reader(smtpItemReader)
            .processor(itemProcessor)
            .writer(itemWriter)
            .taskExecutor(taskExecutor)
            .build();

    return new JobBuilder(JOB_NAME, jobRepository)
            .start(step)
            .build();
  }

}
