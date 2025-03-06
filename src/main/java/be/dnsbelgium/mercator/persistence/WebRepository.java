package be.dnsbelgium.mercator.persistence;

import com.github.f4b6a3.ulid.Ulid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class WebRepository {

  private final String dataLocation;
  private static final Logger logger = LoggerFactory.getLogger(WebRepository.class);
  private final JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());

  public WebRepository(@Value("${mercator.data.location:mercator/data/}") String dataLocation) {
    if (dataLocation == null || dataLocation.isEmpty()) {
      throw new IllegalArgumentException("dataLocation must not be null or empty");
    }
    if (dataLocation.endsWith("/")) {
      this.dataLocation = dataLocation;
    } else {
      this.dataLocation = dataLocation + "/";
    }
    logger.info("dataLocation = [{}]", dataLocation);
  }

  public void toParquet(Path jsonFile) {
    String fileName = Ulid.fast() + ".parquet";
    toParquet(jsonFile, fileName);

  }

  /**
   * Converts the given JSON file to parquet format.
   */
  public void toParquet(Path jsonFile, String fileName) {
    String destination = dataLocation + fileName;
    logger.debug("copying file {} to {}", jsonFile, destination);
    String copy = String.format("copy (select * from '%s') to '%s'", jsonFile, destination);
    logger.debug("copy stmt: {}", copy);
    // TODO: make sure the output parquet matches with what we already have (= exports of the old Mercator)
    //noinspection SqlSourceToSinkFlow
    jdbcClient.sql(copy).update();
    long rowCount = jdbcClient.sql("select count(1) from '" + destination + "'").query(Long.class).single();
    logger.info("parquet file created with {} rows", rowCount);
  }

}
