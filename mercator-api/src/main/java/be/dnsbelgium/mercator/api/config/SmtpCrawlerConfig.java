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
    basePackages = "be.dnsbelgium.mercator.smtp.persistence",
    entityManagerFactoryRef = "smtpEntityManager",
    transactionManagerRef = "smtpTransactionManager"
)
public class SmtpCrawlerConfig {

  @Bean
  @ConfigurationProperties(prefix = "spring.datasource.smtp")
  public DataSource smtpDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean
  public LocalContainerEntityManagerFactoryBean smtpEntityManager() {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    em.setDataSource(smtpDataSource());
    em.setPackagesToScan("be.dnsbelgium.mercator.smtp.persistence");

    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    em.setJpaVendorAdapter(vendorAdapter);

    return em;
  }

  @Bean
  public PlatformTransactionManager smtpTransactionManager() {
    JpaTransactionManager transactionManager = new JpaTransactionManager();
    transactionManager.setEntityManagerFactory(smtpEntityManager().getObject());
    return transactionManager;
  }

}
