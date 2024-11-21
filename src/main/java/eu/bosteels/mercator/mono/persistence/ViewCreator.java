package eu.bosteels.mercator.mono.persistence;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.util.List;

@Component
public class ViewCreator {

  private final VisitRepository visitRepository;
  private final JdbcClient jdbcClient;

  private static final Logger logger = LoggerFactory.getLogger(ViewCreator.class);

  public ViewCreator(DataSource dataSource, VisitRepository visitRepository) {
    this.visitRepository = visitRepository;
    jdbcClient = JdbcClient.create(dataSource);
  }

  //@PostConstruct
  public void createViews() {
    List<String> tableNames  = visitRepository.getTableNames();
    logger.info("tableNames = {}", tableNames);
    for (String tableName : tableNames) {
      createView(tableName);
    }
  }

  private void createView(String tableName) {
    String path = visitRepository.getExportDirectory().getAbsolutePath();
    String ddl  = "create or replace table %s as select * from read_parquet('%s/**/%s.parquet', union_by_name = True)"
        .formatted(tableName, path, tableName);
    jdbcClient.sql(ddl).update();
    logger.info("Created view {}", tableName);
  }

}
