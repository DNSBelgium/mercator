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
    basePackages = "be.dnsbelgium.mercator.vat.crawler.persistence",
    entityManagerFactoryRef = "vatEntityManager",
    transactionManagerRef = "vatTransactionManager"
)
public class VatCrawlerConfig {

  @Bean
  @ConfigurationProperties(prefix = "spring.datasource.vat")
  public DataSource vatDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean
  public LocalContainerEntityManagerFactoryBean vatEntityManager() {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(vatDataSource());
    em.setPackagesToScan("be.dnsbelgium.mercator.vat.crawler.persistence");

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    em.setJpaVendorAdapter(vendorAdapter);

    return em;
  }

  @Bean
  public PlatformTransactionManager vatTransactionManager() {
    JpaTransactionManager transactionManager = new JpaTransactionManager();
    transactionManager.setEntityManagerFactory(vatEntityManager().getObject());
    return transactionManager;
  }

}
