package be.dnsbelgium.mercator.batch;

import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeatures;
import be.dnsbelgium.mercator.persistence.DuckDataSource;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.json.*;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.PathResource;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class JsonTest {

  private static final Logger logger = LoggerFactory.getLogger(JsonTest.class);

  @Test
  public void readJson() throws Exception {
    String inputFileName = "./target/test-outputs/web.json";
    JsonItemReader<WebCrawlResult> jsonItemReader = new JsonItemReader<>();
    jsonItemReader.setResource(new PathResource(inputFileName));
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    JsonObjectReader<WebCrawlResult> jsonObjectReader = new JacksonJsonObjectReader<>(objectMapper, WebCrawlResult.class);
    jsonItemReader.setJsonObjectReader(jsonObjectReader);
    jsonItemReader.open(new ExecutionContext());
    WebCrawlResult webCrawlResult = jsonItemReader.read();
    System.out.println("webCrawlResult = " + webCrawlResult);
  }

  @Test
  public void read() throws Exception {
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(javaTimeModule);
    JacksonJsonObjectReader<WebCrawlResult> jsonObjectReader = new JacksonJsonObjectReader<>(objectMapper, WebCrawlResult.class);
    String inputFileName = "./target/test-outputs/web.json";
    jsonObjectReader.open(new PathResource(inputFileName));
    WebCrawlResult webCrawlResult = jsonObjectReader.read();
    while (webCrawlResult != null) {
      System.out.println("webCrawlResult = " + webCrawlResult);
      webCrawlResult = jsonObjectReader.read();
    }
  }

  @Test
  public void parquetToJson() throws Exception {
    JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());
    // create a parquet file
    jdbcClient.sql("copy (FROM './target/test-outputs/web.json') to 'web.parquet' ");
    // get one trow from the parquet file
    Optional<String> json = jdbcClient
            .sql("select list(to_json(p)) from 'web.parquet' p where visitId = ? limit 1")
            .param("v101")
            .query(String.class)
            .optional();
    if (json.isPresent()) {
      //System.out.println("json = " + json.get());
      JavaTimeModule javaTimeModule = new JavaTimeModule();
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(javaTimeModule);
      JacksonJsonObjectReader<WebCrawlResult> jsonObjectReader = new JacksonJsonObjectReader<>(objectMapper, WebCrawlResult.class);
      jsonObjectReader.open(new ByteArrayResource(json.get().getBytes(StandardCharsets.UTF_8)));
      WebCrawlResult webCrawlResult = jsonObjectReader.read();
      System.out.println("webCrawlResult = " + webCrawlResult);
      if (webCrawlResult != null) {
        webCrawlResult.getHtmlFeatures().forEach(System.out::println);
      }

    }
  }


  @Test
  public void readObject() throws JsonProcessingException {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());
    // create a parquet file
    jdbcClient.sql("copy (FROM './target/test-outputs/web.json') to 'web.parquet' ");
    // get one row from the parquet file
    Optional<String> json = jdbcClient
            .sql("select to_json(p) from 'web.parquet' p where visitId = ? limit 1")
            .param("v101")
            .query(String.class)
            .optional();
    if (json.isPresent()) {
      WebCrawlResult webCrawlResult = objectMapper.readValue(json.get(), WebCrawlResult.class);
      logger.info("webCrawlResult = {}", webCrawlResult);
      for (HtmlFeatures htmlFeature : webCrawlResult.getHtmlFeatures()) {
        logger.info("htmlFeature = {}", htmlFeature);

      }
    }


  }
}
