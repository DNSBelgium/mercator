package be.dnsbelgium.mercator.batch;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.vat.crawler.persistence.WebCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.FlatFileItemWriter;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.batch.item.file.builder.FlatFileItemWriterBuilder;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.JsonFileItemWriter;
import org.springframework.batch.item.json.builder.JsonFileItemWriterBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.task.VirtualThreadTaskExecutor;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;
import java.time.format.DateTimeFormatter;

@SuppressWarnings("SpringElInspection")
@Configuration
@EnableBatchProcessing
public class JobConfiguration {

  private static final Logger logger = LoggerFactory.getLogger(JobConfiguration.class);

  @Bean
  @StepScope
  public FlatFileItemReader<VisitRequest> visitRequestItemReader(@Value("#{jobParameters[inputFile]}") Resource resource) {
    logger.info("creating visitRequestItemReader");
    return new FlatFileItemReaderBuilder<VisitRequest>()
            .name("visitRequestReader")
            .resource(resource)
            .delimited()
            .names("domainName", "visitId")
            .targetType(VisitRequest.class)
            .build();
  }



  @Bean
  @StepScope
  public FlatFileItemWriter<WebCrawlResult> webCrawlResultItemWriter(@Value("#{jobParameters[outputFile]}") WritableResource resource) {
    logger.info("resource = {}", resource);
    FlatFileItemWriter<WebCrawlResult> writer = new FlatFileItemWriterBuilder<WebCrawlResult>()
            .name("webCrawlResultItemWriter")
            .resource(resource)
            .delimited()
            .names("visitId", "domainName", "startUrl", "matchingUrl", "crawlStarted", "crawlFinished", "vatValues", "visitedUrls")
            .headerCallback(
                    w -> w.write("visitId,domainName,startUrl,matchingUrl,crawlStarted,crawlFinished,vatValues,visitedUrls"))
            .build();
    logger.info("writer = {}", writer);
    return writer;
  }

  @Bean
  public JsonFileItemWriter<WebCrawlResult> webCrawlResultJsonFileItemWriter() {

    ObjectMapper objectMapper = new ObjectMapper();
    //customize objectMapper if needed
    objectMapper.registerModule(javaTimeModule());
    JacksonJsonObjectMarshaller<WebCrawlResult> jsonObjectMarshaller
            = new JacksonJsonObjectMarshaller<>(objectMapper);

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    WritableResource outputJsonFile = (WritableResource) resolver.getResource("file:output-web.json");

    return new JsonFileItemWriterBuilder<WebCrawlResult>()
            .name("WebCrawlResultWriter")
            .jsonObjectMarshaller(jsonObjectMarshaller)
            //.jsonObjectMarshaller(new GsonJsonObjectMarshaller<>()) // If Gson is used
            .resource(outputJsonFile)
            .build();
  }


  @Bean
  public Job webJob(JobRepository jobRepository,
                    JdbcTransactionManager transactionManager,
                    ItemReader<VisitRequest> itemReader,
                    WebProcessor processor,
                    JsonFileItemWriter<WebCrawlResult> itemWriter)
  {
    Step step = new StepBuilder("web", jobRepository)
            .<VisitRequest, WebCrawlResult>chunk(10, transactionManager)
            .reader(itemReader)
            .taskExecutor(new VirtualThreadTaskExecutor("web-virtual-thread"))
            .processor(processor)
            .writer(itemWriter)
            .faultTolerant().retry(Exception.class).retryLimit(5)
            .build();

    return new JobBuilder("web", jobRepository)
            .start(step)
            .build();
  }

  public static final String DATETIME_FORMAT = "dd-MM-yyyy HH:mm";

  @Bean
  public JavaTimeModule javaTimeModule() {
    JavaTimeModule module = new JavaTimeModule();
    LocalDateTimeSerializer LOCAL_DATETIME_SERIALIZER = new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATETIME_FORMAT));
    module.addSerializer(LOCAL_DATETIME_SERIALIZER);
    return module;
  }

  @Bean
  public JobRepositoryFactoryBean jobRepositoryFactoryBean(DataSource dataSource, PlatformTransactionManager transactionManager) throws Exception {
    JobRepositoryFactoryBean factoryBean = new JobRepositoryFactoryBean();
    factoryBean.setDatabaseType("POSTGRES");
    factoryBean.setDataSource(dataSource);
    factoryBean.setTransactionManager(transactionManager);
    factoryBean.afterPropertiesSet();
    return factoryBean;
  }

  @Bean
  public JobRepository jobRepository(JobRepositoryFactoryBean factoryBean) throws Exception {
    return factoryBean.getObject();
  }


}
