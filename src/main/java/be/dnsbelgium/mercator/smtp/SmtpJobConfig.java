package be.dnsbelgium.mercator.smtp;

import be.dnsbelgium.mercator.batch.BatchConfig;
import be.dnsbelgium.mercator.batch.JsonItemWriter;
import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.persistence.SmtpRepository;
import be.dnsbelgium.mercator.smtp.dto.SmtpVisit;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.file.Path;

@SuppressWarnings("SpringElInspection")
@Configuration
public class SmtpJobConfig {

  private static final Logger logger = LoggerFactory.getLogger(SmtpJobConfig.class);
  private static final String JOB_NAME = "smtp";

  @Value("${smtp.corePoolSize:100}")
  private int corePoolSize;

  @Value("${smtp.maxPoolSize:100}")
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
  public JsonItemWriter<SmtpVisit> smtpItemWriter(
          BatchConfig batchConfig, SmtpRepository repository, ObjectMapper objectMapper) {
    Path outputDirectory = batchConfig.outputDirectoryFor(JOB_NAME);
    return new JsonItemWriter<>(repository, objectMapper, outputDirectory, SmtpVisit.class);
  }

  @Bean
  @Qualifier(JOB_NAME)
  public ThreadPoolTaskExecutor smtpTaskExecutor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(-1);
    executor.setThreadNamePrefix(JOB_NAME);
    logger.info("executor corePoolSize={} maxPoolSize={}", corePoolSize, maxPoolSize);
    return executor;
  }

  @Bean(name = "smtpJob")
  @ConditionalOnProperty(name = "job.smtp.enabled", havingValue = "true")
  public Job smtpJob(JobRepository jobRepository,
                     PlatformTransactionManager transactionManager,
                     ItemReader<VisitRequest> smtpItemReader,
                     SmtpCrawler smtpCrawler,
                     JsonItemWriter<SmtpVisit> itemWriter,
                     @Qualifier(JOB_NAME) ThreadPoolTaskExecutor taskExecutor) {
    logger.info("creating {}", JOB_NAME);

    // TODO: try a VirtualThreadExecutor since time outs for SMTP are very high

    // throttleLimit is deprecated but alternative not very clear ...
    @SuppressWarnings("removal")
    Step step = new StepBuilder(JOB_NAME, jobRepository)
            .<VisitRequest, SmtpVisit>chunk(chunkSize, transactionManager)
            .reader(smtpItemReader)
            .processor(smtpCrawler)
            .writer(itemWriter)
            .taskExecutor(taskExecutor)
            .throttleLimit(maxPoolSize - 10)
            .build();

    return new JobBuilder(JOB_NAME, jobRepository)
            .start(step)
            .build();
  }

}
