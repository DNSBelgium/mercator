package be.dnsbelgium.mercator.smtp;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.smtp.dto.SmtpVisit;
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.jdbc.support.JdbcTransactionManager;

@SuppressWarnings("SpringElInspection")
@Configuration
public class SmtpJobConfig {

  private static final Logger logger = LoggerFactory.getLogger(SmtpJobConfig.class);

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
  public JsonFileItemWriter<SmtpVisit> smtpJsonFileItemWriter(
          ObjectMapper objectMapper,
          @Value("#{jobParameters[outputFile]}") WritableResource resource) {
    JacksonJsonObjectMarshaller<SmtpVisit> jsonObjectMarshaller
            = new JacksonJsonObjectMarshaller<>(objectMapper);
    return new JsonFileItemWriterBuilder<SmtpVisit>()
            .name("smtp-writer")
            .jsonObjectMarshaller(jsonObjectMarshaller)
            .resource(resource)
            .build();
  }

  @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
  @Bean(name = "smtpJob")
  @ConditionalOnProperty(name = "job.smtp.enabled", havingValue = "true", matchIfMissing = false)
  public Job smtpJob(JobRepository jobRepository,
                    JdbcTransactionManager transactionManager,
                    ItemReader<VisitRequest> itemReader,
                    SmtpCrawler smtpCrawler,
                    JsonFileItemWriter<SmtpVisit> itemWriter) {
    logger.info("creating smtpJob");
    Step step = new StepBuilder("smtp", jobRepository)
            .<VisitRequest, SmtpVisit>chunk(10, transactionManager)
            .reader(itemReader)
            //.taskExecutor(new VirtualThreadTaskExecutor("smtp-virtual-thread"))
            .processor(smtpCrawler)
            .writer(itemWriter)
            .build();

    return new JobBuilder("smtp", jobRepository)
            .start(step)
            .build();
  }

}
