package be.dnsbelgium.mercator.batch;

import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.SingleConnectionDataSource;

import javax.sql.DataSource;

@Configuration
public class BatchBeans {

  @Bean
  ResourcelessJobRepository jobRepository() {
    return new ResourcelessJobRepository();
  }

  @Bean
  public ResourcelessTransactionManager transactionManager() {
    return new ResourcelessTransactionManager();
  }

  // For now, Spring Batch insists on having a DataSource bean
  // See
  //  https://github.com/spring-projects/spring-batch/issues/4825
  //  and
  // https://github.com/spring-projects/spring-batch/issues/4718

  @Bean
  public DataSource dataSource() {
    String url = "jdbc:duckdb:";
    return new SingleConnectionDataSource(url, true);
  }
}
