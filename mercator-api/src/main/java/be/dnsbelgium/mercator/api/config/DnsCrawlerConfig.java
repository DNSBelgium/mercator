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
    basePackages = "be.dnsbelgium.mercator.dns.persistence",
    entityManagerFactoryRef = "dnsEntityManager",
    transactionManagerRef = "dnsTransactionManager"
)
public class DnsCrawlerConfig {

  @Bean
  @ConfigurationProperties(prefix = "spring.datasource.dns")
  public DataSource dnsDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean
  public LocalContainerEntityManagerFactoryBean dnsEntityManager() {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(dnsDataSource());
    em.setPackagesToScan("be.dnsbelgium.mercator.dns.persistence");

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    em.setJpaVendorAdapter(vendorAdapter);

    return em;
  }

  @Bean
  public PlatformTransactionManager dnsTransactionManager() {
    JpaTransactionManager transactionManager = new JpaTransactionManager();
    transactionManager.setEntityManagerFactory(dnsEntityManager().getObject());
    return transactionManager;
  }

}
