package be.dnsbelgium.mercator.feature.extraction;

import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeatures;
import be.dnsbelgium.mercator.persistence.DuckDataSource;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.File;
import java.util.List;

public class RunFeatureExtractionBatchTest {

  private static final Logger logger = LoggerFactory.getLogger(RunFeatureExtractionBatchTest.class);

  public record Sample(String domain_name, String filename, String content, String finalUrl, String label){}



  public record Record(HtmlFeatures feature, Sample sample){}

  // This takes 48 seconds for 4.333 rows.

  @Test
  @Disabled
  public void runBatch() {
    logger.info("Running batch...");
    DuckDataSource dataSource = DuckDataSource.memory();
    JdbcClient jdbcClient = JdbcClient.create(dataSource);

    String DATA_DIR = System.getenv("DATA_DIR");
    logger.info("DATA_DIR = {}", DATA_DIR);
    if (DATA_DIR == null) {
      throw new RuntimeException("DATA_DIR environment variable not set");
    }


    String table = """
        create table if not exists feats(
          domain_name varchar,
          filename    varchar,
          content     varchar,
          final_url   varchar,
          label       varchar,
          features    json
        )
        """;

    jdbcClient.sql(table).update();

    ObjectMapper mapper = new ObjectMapper();
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    HtmlFeatureExtractor featureExtractor = new HtmlFeatureExtractor(registry, false);

    String query = """
        select domain_name, filename, content, final_url, label
        from '%s/labeled_html.parquet'
        """.formatted(DATA_DIR);
    logger.info("query = {}", query);

    List<Sample> rows = jdbcClient
        .sql(query)
        .query(Sample.class)
        .list();

    logger.info("rows = {}", rows.size());

    @SuppressWarnings("SqlResolve") String insert = """
        insert into feats(domain_name, filename, content, final_url, label, features)
        values (:domain_name, :filename, :content, :final_url, :label, :features)
        """;


    String json_directory = "%s/html_features".formatted(DATA_DIR);
    new File(json_directory).mkdirs();
    logger.info("created directory {}", json_directory);

    for (Sample row : rows) {
      //logger.info("filename={} finaleUrl={} label={}", row.filename, row.finalUrl, row.label);
      try {
        HtmlFeatures feats = featureExtractor.extractFromHtml(row.content, row.finalUrl, row.domain_name);

        Record record = new Record(feats, row);

        String json = mapper.writeValueAsString(record);

        File file = new File(json_directory,row.filename + ".json");
        //logger.info("Saving in {}", file);

        mapper.writeValue(file, record);

        jdbcClient
            .sql(insert)
            .param("domain_name", row.domain_name)
            .param("filename", row.filename)
            .param("content", row.content)
            .param("final_url", row.finalUrl)
            .param("label", row.label)
            .param("features", json)
            .update();
      } catch (Exception e) {
        
        logger.error("Error processing file {}", row.filename, e);
        throw new RuntimeException(e);
      }
    }
    String copy = "copy (from feats) to '%s/features.parquet'".formatted(DATA_DIR);
    logger.info("copy = {}", copy);
    jdbcClient.sql(copy).update();
    logger.info("copy to parquet done");

    String countRows = "select count(1) from '%s/features.parquet'".formatted(DATA_DIR);
    int rowCount = jdbcClient.sql(countRows).query(Integer.class).single();
    logger.info("rowCount = {}", rowCount);


  }

}
