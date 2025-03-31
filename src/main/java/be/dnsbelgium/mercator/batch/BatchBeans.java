package be.dnsbelgium.mercator.batch;

import org.springframework.batch.core.repository.support.ResourcelessJobRepository;
import org.springframework.batch.support.transaction.ResourcelessTransactionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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

}
