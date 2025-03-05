package be.dnsbelgium.mercator.scheduling;

import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.item.json.JacksonJsonObjectMarshaller;
import org.springframework.batch.item.json.JacksonJsonObjectReader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableBatchProcessing
public class BatchConfig {

  @Bean
  public JacksonJsonObjectReader<WebCrawlResult> webCrawlResultReader(ObjectMapper objectMapper) {
    return new JacksonJsonObjectReader<>(objectMapper, WebCrawlResult.class);
  }

  @Bean
  public JacksonJsonObjectMarshaller<WebCrawlResult> webCrawlResultReaderMarshaller(ObjectMapper objectMapper) {
    return new JacksonJsonObjectMarshaller<>(objectMapper);
  }

  @Bean
  public JacksonJsonObjectMarshaller<TlsCrawlResult> tlsCrawlResultReaderMarshaller(ObjectMapper objectMapper) {
    return new JacksonJsonObjectMarshaller<>(objectMapper);
  }

}
