package be.dnsbelgium.mercator.dns;

import be.dnsbelgium.mercator.batch.BatchConfig;
import be.dnsbelgium.mercator.batch.JsonItemWriter;
import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.dns.domain.DnsCrawlService;
import be.dnsbelgium.mercator.dns.dto.DnsCrawlResult;
import be.dnsbelgium.mercator.persistence.DnsRepository;
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
public class DnsJobConfig {

  private static final Logger logger = LoggerFactory.getLogger(DnsJobConfig.class);
  private static final String JOB_NAME = "dns";

  @Value("${dns.chunkSize:1000}")
  private int chunkSize;

  @Value("${dns.throttleLimit:200}")
  private int throttleLimit;

  @Bean
  @StepScope
  @SuppressWarnings({"SpringElInspection"})
  public FlatFileItemReader<VisitRequest> dnsItemReader(@Value("#{jobParameters[inputFile]}") Resource resource) {
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
  public JsonItemWriter<DnsCrawlResult> dnsItemWriter(
          BatchConfig batchConfig, DnsRepository repository, ObjectMapper objectMapper) {
    Path outputDirectory = batchConfig.outputDirectoryFor(JOB_NAME);
    return new JsonItemWriter<>(repository, objectMapper, outputDirectory, DnsCrawlResult.class);
  }

  @Bean(name = "dnsJob")
  @ConditionalOnProperty(name = "job.dns.enabled", havingValue = "true")
  public Job dnsJob(JobRepository jobRepository,
                    PlatformTransactionManager transactionManager,
                    ItemReader<VisitRequest> itemReader,
                    DnsCrawlService dnsCrawler,
                    ItemWriter<DnsCrawlResult> itemWriter) {
    logger.info("creating dnsJob");
    // throttleLimit method is deprecated but alternative is not well documented
    @SuppressWarnings("removal")
    Step step = new StepBuilder(JOB_NAME, jobRepository)
            .<VisitRequest, DnsCrawlResult>chunk(chunkSize, transactionManager)
            .reader(itemReader)
            .taskExecutor(new VirtualThreadTaskExecutor(JOB_NAME + "-virtual"))
            .throttleLimit(throttleLimit)
            .processor(dnsCrawler)
            .writer(itemWriter)
            .build();

    return new JobBuilder(JOB_NAME, jobRepository)
            .start(step)
            .build();
  }


}
