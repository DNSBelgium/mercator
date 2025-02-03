package be.dnsbelgium.mercator.scheduling;

import be.dnsbelgium.mercator.persistence.DuckDataSource;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.repository.support.JobRepositoryFactoryBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class BatchConfig {

  private static final Logger logger = LoggerFactory.getLogger(BatchConfig.class);

  @Bean
  public JobRepositoryFactoryBean jobRepositoryFactoryBean(DataSource dataSource, PlatformTransactionManager transactionManager) throws Exception {
    JobRepositoryFactoryBean factoryBean = new JobRepositoryFactoryBean();
    // TODO: use a real postgres database, right now we use duckdb disguised as postgres
    factoryBean.setDatabaseType("POSTGRES");
    factoryBean.setDataSource(dataSource);
    factoryBean.setTransactionManager(transactionManager);
    factoryBean.afterPropertiesSet();
    return factoryBean;
  }

  @Bean
  public JobRepository jobRepository(JobRepositoryFactoryBean factoryBean) throws Exception {
    return factoryBean.getObject();
  }

  @Bean
  public DataSource dataSource() {
    DuckDataSource duckDataSource = new DuckDataSource();
    duckDataSource.setUrl("jdbc:duckdb:batch.duckdb");
    duckDataSource.init();
    return duckDataSource;
  }

  @Bean
  public JdbcTransactionManager transactionManager(DataSource dataSource) {
    return new JdbcTransactionManager(dataSource);
  }

  @Bean
  public BatchDatabase batchDatabase(DataSource dataSource) {
    return new BatchDatabase(dataSource);
  }

  @Bean
  public JavaTimeModule javaTimeModule() {
    JavaTimeModule module = new JavaTimeModule();
//    LocalDateTimeSerializer LOCAL_DATETIME_SERIALIZER = new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATETIME_FORMAT));
//    module.addSerializer(LOCAL_DATETIME_SERIALIZER);
    logger.info("module = {}", module);
    return module;
  }

}
