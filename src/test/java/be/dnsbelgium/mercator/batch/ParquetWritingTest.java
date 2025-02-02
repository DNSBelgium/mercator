package be.dnsbelgium.mercator.batch;

import be.dnsbelgium.mercator.persistence.DuckDataSource;
import org.duckdb.DuckDBAppender;
import org.duckdb.DuckDBArray;
import org.duckdb.DuckDBConnection;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

@SuppressWarnings("SqlDialectInspection")
public class ParquetWritingTest {

  DuckDataSource duckDataSource = DuckDataSource.memory();
  JdbcClient jdbcClient = JdbcClient.create(duckDataSource);
  private static final Logger logger = LoggerFactory.getLogger(ParquetWritingTest.class);

  @Test
  public void arrays() throws SQLException {
    createTable();
    jdbcClient
            .sql("insert into my_array(id, values) values (1, ['abc', 'def'])")
            .update();
    printRows();
  }

  @Test
  public void arraysWithAppender() throws SQLException {
    createTable();
    DuckDBConnection cnx = duckDataSource.connection();
    DuckDBAppender appender = new DuckDBAppender(cnx, "main", "my_array");
    appender.beginRow();
    appender.append(100);
    appender.append("[abc, def, o'neil]");
    appender.endRow();
    appender.close();
    printRows();
  }

  private void createTable() {
    jdbcClient
            .sql("create or replace table my_array(id int, values varchar[])")
            .update();
  }

  private void printRows() throws SQLException {
    List<Map<String, Object>> rows = jdbcClient.sql("select * from my_array").query().listOfRows();
    logger.info("rows found: {}", rows);
    for (Map<String, Object> row : rows) {
      logger.info("row = {}", row);
      logger.info("row.id = {}", row.get("id"));
      logger.info("row.values.class = {}", row.get("values").getClass());
      DuckDBArray duckDBArray = (DuckDBArray) row.get("values");
      ResultSet rs = duckDBArray.getResultSet();
      while (rs.next()) {
        int index = rs.getInt(1);
        String v = rs.getString(2);
        logger.info("index={} v = {}", index, v);
      }
    }
  }

  @Test
  public void json() {

  }

}
