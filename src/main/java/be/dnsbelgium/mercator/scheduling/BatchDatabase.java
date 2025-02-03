package be.dnsbelgium.mercator.scheduling;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;

@Component
public class BatchDatabase {

  private static final Logger logger = LoggerFactory.getLogger(BatchDatabase.class);

  private final ResourceLoader resourceLoader;
  private final DataSource dataSource;
  private final ResourceDatabasePopulator resourceDatabasePopulator;

  @Autowired
  public BatchDatabase(DataSource dataSource) {
    this.resourceLoader = new DefaultResourceLoader();
    this.dataSource = dataSource;
    this.resourceDatabasePopulator = new ResourceDatabasePopulator();
  }

  private void addScript(String location) {
    resourceDatabasePopulator.addScript(resourceLoader.getResource(location));
  }

  @PostConstruct
  public void init() {
    logger.info("Initializing BatchDatabase");
    // TODO With a real database, we should keep the tables between runs
    addScript("classpath:org/springframework/batch/core/schema-drop-postgresql.sql");
    addScript("classpath:org/springframework/batch/core/schema-postgresql.sql");
    DatabasePopulatorUtils.execute(resourceDatabasePopulator, dataSource);
    logger.info("DONE Initializing DB");
  }

}
