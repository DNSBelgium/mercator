package be.dnsbelgium.mercator.tls;

import be.dnsbelgium.mercator.common.VisitRequest;
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
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.JsonFileItemWriter;
import org.springframework.batch.item.json.builder.JsonFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.jdbc.support.JdbcTransactionManager;

@SuppressWarnings({"SpringElInspection", "SpringJavaInjectionPointsAutowiringInspection"})
@Configuration
public class TlsJobConfig {

  private static final Logger logger = LoggerFactory.getLogger(TlsJobConfig.class);

  @Bean
  @StepScope
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
  public JacksonJsonObjectMarshaller<TlsCrawlResult> tlsCrawlResultReaderMarshaller(ObjectMapper objectMapper) {
    return new JacksonJsonObjectMarshaller<>(objectMapper);
  }

  @Bean
  @StepScope
  public JsonFileItemWriter<TlsCrawlResult> tlsJsonFileItemWriter(
          @Value("#{jobParameters[outputFile]}") WritableResource resource,
          JacksonJsonObjectMarshaller<TlsCrawlResult> marshaller) {
    return new JsonFileItemWriterBuilder<TlsCrawlResult>()
            .name("tls-writer")
            .jsonObjectMarshaller(marshaller)
            .resource(resource)
            .build();
  }

  @Bean(name = "tlsJob")
  @ConditionalOnProperty(name = "job.tls.enabled", havingValue = "true")
  public Job tlsJob(JobRepository jobRepository,
                    JdbcTransactionManager transactionManager,
                    ItemReader<VisitRequest> itemReader,
                    TlsCrawler tlsCrawler,
                    JsonFileItemWriter<TlsCrawlResult> itemWriter) {
    logger.info("creating tlsJob");
    Step step = new StepBuilder("tls", jobRepository)
            .<VisitRequest, TlsCrawlResult>chunk(10, transactionManager)
            .reader(itemReader)
            .taskExecutor(new VirtualThreadTaskExecutor("tls-virtual-thread"))
            .processor(tlsCrawler)
            .writer(itemWriter)
            .build();

    return new JobBuilder("tls", jobRepository)
            .start(step)
            .build();
  }

}
