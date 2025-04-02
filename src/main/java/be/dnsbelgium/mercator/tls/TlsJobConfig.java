package be.dnsbelgium.mercator.tls;

import be.dnsbelgium.mercator.batch.BatchConfig;
import be.dnsbelgium.mercator.batch.JsonItemWriter;
import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.persistence.TlsRepository;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import be.dnsbelgium.mercator.tls.ports.TlsCrawler;
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
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.transaction.PlatformTransactionManager;

import java.nio.file.Path;

@Configuration
public class TlsJobConfig {

  private static final Logger logger = LoggerFactory.getLogger(TlsJobConfig.class);
  private static final String JOB_NAME = "tls";

  @Value("${tls.chunkSize:1000}")
  private int chunkSize;

  @Value("${tls.throttleLimit:200}")
  private int throttleLimit;

  @Bean
  @StepScope
  @SuppressWarnings({"SpringElInspection"})
  public FlatFileItemReader<VisitRequest> tlsItemReader(@Value("#{jobParameters[inputFile]}") Resource resource) {
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
  public JsonItemWriter<TlsCrawlResult> tlsItemWriter(
          BatchConfig batchConfig, TlsRepository repository, ObjectMapper objectMapper) {
    Path outputDirectory = batchConfig.outputDirectoryFor(JOB_NAME);
    return new JsonItemWriter<>(repository, objectMapper, outputDirectory, TlsCrawlResult.class);
  }

  @Bean(name = "tlsJob")
  @ConditionalOnProperty(name = "job.tls.enabled", havingValue = "true")
  public Job tlsJob(JobRepository jobRepository,
                    PlatformTransactionManager transactionManager,
                    ItemReader<VisitRequest> itemReader,
                    TlsCrawler tlsCrawler,
                    ItemWriter<TlsCrawlResult> itemWriter,
                    TlsParquetMaker tlsParquetMaker) {
    logger.info("creating tlsJob");
    // throttleLimit method is deprecated but alternative is not well documented
    @SuppressWarnings("removal")
    Step step = new StepBuilder(JOB_NAME, jobRepository)
            .<VisitRequest, TlsCrawlResult>chunk(chunkSize, transactionManager)
            .reader(itemReader)
            .taskExecutor(new VirtualThreadTaskExecutor(JOB_NAME + "-virtual"))
            .throttleLimit(throttleLimit)
            .processor(tlsCrawler)
            .writer(itemWriter)
            .build();

    return new JobBuilder(JOB_NAME, jobRepository)
            .start(step)
            .listener(tlsParquetMaker)
            .build();
  }


}
