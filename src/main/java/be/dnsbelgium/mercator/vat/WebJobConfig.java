package be.dnsbelgium.mercator.vat;

import be.dnsbelgium.mercator.batch.BatchConfig;
import be.dnsbelgium.mercator.batch.JsonItemWriter;
import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.persistence.WebRepository;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.*;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.nio.file.Path;

@Configuration
public class WebJobConfig {

  private static final Logger logger = LoggerFactory.getLogger(WebJobConfig.class);

  private static final String JOB_NAME = "web";

  @Value("${web.corePoolSize:100}")
  private int corePoolSize;

  @Value("${web.maxPoolSize:100}")
  private int maxPoolSize;

  @Value("${web.chunkSize:1000}")
  private int chunkSize;

  @Bean
  @Qualifier(JOB_NAME)
  public ThreadPoolTaskExecutor webTaskExecutor() {
    var executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(-1);
    executor.setThreadNamePrefix(JOB_NAME + "-");
    logger.info("{} executor corePoolSize={} maxPoolSize={}", JOB_NAME, corePoolSize, maxPoolSize);
    return executor;
  }

  @SuppressWarnings("SpringElInspection")
  @Bean
  @StepScope
  public FlatFileItemReader<VisitRequest> itemReader(@Value("#{jobParameters[inputFile]}") Resource resource) {
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
  public JsonItemWriter<WebCrawlResult> webItemWriter(
          BatchConfig batchConfig, WebRepository repository, ObjectMapper objectMapper) {
    Path outputDirectory = batchConfig.outputDirectoryFor(JOB_NAME);
    return new JsonItemWriter<>(repository, objectMapper, outputDirectory, WebCrawlResult.class);
  }

  @Bean(name = JOB_NAME + "Job")
  @ConditionalOnProperty(name = "job.web.enabled", havingValue = "true")
  public Job webJob(ResourcelessJobRepository jobRepository,
                    ResourcelessTransactionManager transactionManager,
                    ItemReader<VisitRequest> itemReader,
                    WebProcessor processor,
                    JsonItemWriter<WebCrawlResult> jsonItemWriter,
                    @Qualifier(JOB_NAME) ThreadPoolTaskExecutor taskExecutor
  ) {
    @SuppressWarnings("removal")
    Step step = new StepBuilder(JOB_NAME, jobRepository)
            .<VisitRequest, WebCrawlResult>chunk(chunkSize, transactionManager)
            .reader(itemReader)
            .processor(processor)
            .writer(jsonItemWriter)
            .taskExecutor(taskExecutor)
            .throttleLimit(maxPoolSize - 10)
            .build();

    return new JobBuilder(JOB_NAME, jobRepository)
            .start(step)
            .build();
  }

}
