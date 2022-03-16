package be.dnsbelgium.mercator.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(
    basePackages = "be.dnsbelgium.mercator.content.persistence",
    entityManagerFactoryRef = "contentEntityManager",
    transactionManagerRef = "contentTransactionManager"
)
public class ContentCrawlerConfig {

  @Bean
  @ConfigurationProperties(prefix = "spring.datasource.content")
  public DataSource contentDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean
  public LocalContainerEntityManagerFactoryBean contentEntityManager() {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(contentDataSource());
    em.setPackagesToScan("be.dnsbelgium.mercator.content.persistence");

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    em.setJpaVendorAdapter(vendorAdapter);

    return em;
  }

  @Bean
  public PlatformTransactionManager contentTransactionManager() {
    JpaTransactionManager transactionManager = new JpaTransactionManager();
    transactionManager.setEntityManagerFactory(contentEntityManager().getObject());
    return transactionManager;
  }

}
