package be.dnsbelgium.mercator.batch;

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
public class Db {

  private static final Logger logger = LoggerFactory.getLogger(Db.class);

  private final ResourceLoader resourceLoader;
  private final DataSource dataSource;
  private final ResourceDatabasePopulator resourceDatabasePopulator;

  @Autowired
  public Db(DataSource dataSource) {
    this.resourceLoader = new DefaultResourceLoader();
    this.dataSource = dataSource;
    this.resourceDatabasePopulator = new ResourceDatabasePopulator();
  }

  private void addScript(String location) {
    resourceDatabasePopulator.addScript(resourceLoader.getResource(location));

  }

  @PostConstruct
  public void init() {
    logger.info("Initializing DB");
    addScript("classpath:org/springframework/batch/core/schema-drop-postgresql.sql");
    addScript("classpath:org/springframework/batch/core/schema-postgresql.sql");
    DatabasePopulatorUtils.execute(resourceDatabasePopulator, dataSource);
    logger.info("DONE Initializing DB");
  }


}
