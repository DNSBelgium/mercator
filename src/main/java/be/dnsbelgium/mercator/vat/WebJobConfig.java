package be.dnsbelgium.mercator.vat;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
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
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.jdbc.support.JdbcTransactionManager;

@SuppressWarnings("SpringElInspection")
@Configuration
public class WebJobConfig {

  private static final Logger logger = LoggerFactory.getLogger(WebJobConfig.class);

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
  @StepScope
  public JsonFileItemWriter<WebCrawlResult> webCrawlResultJsonFileItemWriter(
          @Value("#{jobParameters[outputFile]}") WritableResource resource,
          JavaTimeModule javaTimeModule) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(javaTimeModule);
    JacksonJsonObjectMarshaller<WebCrawlResult> jsonObjectMarshaller
            = new JacksonJsonObjectMarshaller<>(objectMapper);

    return new JsonFileItemWriterBuilder<WebCrawlResult>()
            .name("WebCrawlResultWriter")
            .jsonObjectMarshaller(jsonObjectMarshaller)
            .resource(resource)
            .build();
  }

  @Bean(name = "webJob")
  public Job webJob(JobRepository jobRepository,
                    JdbcTransactionManager transactionManager,
                    ItemReader<VisitRequest> itemReader,
                    WebProcessor processor,
                    JsonFileItemWriter<WebCrawlResult> itemWriter) {
    logger.info("creating webJob");
    Step step = new StepBuilder("web", jobRepository)
            .<VisitRequest, WebCrawlResult>chunk(10, transactionManager)
            .reader(itemReader)
            //.taskExecutor(new VirtualThreadTaskExecutor("web-virtual-thread"))
            .processor(processor)
            .writer(itemWriter)
            //.faultTolerant().retry(Exception.class).retryLimit(5)
            .build();

    return new JobBuilder("web", jobRepository)
            .start(step)
            .build();
  }


}
