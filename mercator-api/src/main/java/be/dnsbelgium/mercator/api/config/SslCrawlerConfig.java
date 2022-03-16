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
    basePackages = "be.dnsbelgium.mercator.ssl.crawler.persistence",
    entityManagerFactoryRef = "sslEntityManager",
    transactionManagerRef = "sslTransactionManager"
)
public class SslCrawlerConfig {

  @Bean
  @ConfigurationProperties(prefix = "spring.datasource.ssl")
  public DataSource sslDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean
  public LocalContainerEntityManagerFactoryBean sslEntityManager() {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(sslDataSource());
    em.setPackagesToScan("be.dnsbelgium.mercator.ssl.crawler.persistence");

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    em.setJpaVendorAdapter(vendorAdapter);

    return em;
  }

  @Bean
  public PlatformTransactionManager sslTransactionManager() {
    JpaTransactionManager transactionManager = new JpaTransactionManager();
    transactionManager.setEntityManagerFactory(sslEntityManager().getObject());
    return transactionManager;
  }
}
