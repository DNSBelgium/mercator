package be.dnsbelgium.mercator.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.testcontainers.containers.PostgreSQLContainer;

public class PostgreSqlContainer extends PostgreSQLContainer<PostgreSqlContainer> {

  private static final Logger logger = LoggerFactory.getLogger(PostgreSqlContainer.class);

  private static final String IMAGE_VERSION = "postgres:16.4";
  private static PostgreSqlContainer container;

  private PostgreSqlContainer() {
    super(IMAGE_VERSION);
  }

  public static PostgreSqlContainer getInstance() {
    if (container == null) {
      container = new PostgreSqlContainer();
    }
    return container;
  }

  // The code below prevent the container to be stopped, sharing the instances over multiple test classes.
  static {
    PostgreSqlContainer instance = getInstance();
    instance.start();
  }

  @Override
  public void start() {
    logger.info("PostgreSQL is " + (super.isRunning() ? "" : "not ") + "running");
    super.start();
  }

  @Override
  public void stop() {
    // nothing
    logger.info("Stopping PostgreSQL");
  }

  public void setDatasourceProperties(DynamicPropertyRegistry registry, String schema) {
    logger.info("Registering datasource properties of container. schema={}", schema);
    registry.add("spring.datasource.url",      () -> getJdbcUrl() + "&currentSchema=" + schema);
    registry.add("spring.datasource.username", this::getUsername);
    registry.add("spring.datasource.password", this::getPassword);
  }

}
