package be.dnsbelgium.mercator.vat;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.vat.crawler.persistence.WebCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
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
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

@SuppressWarnings("SpringElInspection")
@Configuration
public class WebJobConfig {

  private static final Logger logger = LoggerFactory.getLogger(WebJobConfig.class);

  @Bean
  @StepScope
  public FlatFileItemReader<VisitRequest> itemReader(@Value("#{jobParameters[inputFile]}") Resource resource) {
    logger.info("creating FlatFileItemReader for resource {}", resource);
    return new FlatFileItemReaderBuilder<VisitRequest>().name("itemReader")
            .resource(resource)
            .delimited()
            .names("visitId", "domainName")
            .targetType(VisitRequest.class)
            .build();
  }

  @Bean
  @StepScope
  public JsonFileItemWriter<WebCrawlResult> webCrawlResultJsonFileItemWriter(JavaTimeModule javaTimeModule) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(javaTimeModule);
    JacksonJsonObjectMarshaller<WebCrawlResult> jsonObjectMarshaller
            = new JacksonJsonObjectMarshaller<>(objectMapper);

    PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
    WritableResource outputJsonFile = (WritableResource) resolver.getResource("file:WebCrawlResult.json");

    return new JsonFileItemWriterBuilder<WebCrawlResult>()
            .name("WebCrawlResultWriter")
            .jsonObjectMarshaller(jsonObjectMarshaller)
            .resource(outputJsonFile)
            .build();
  }

}
