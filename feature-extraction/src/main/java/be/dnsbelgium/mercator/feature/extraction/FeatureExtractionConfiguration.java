package be.dnsbelgium.mercator.feature.extraction;

import be.dnsbelgium.mercator.common.messaging.jms.JmsConfig;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
@EnableJpaRepositories(
    basePackages = "be.dnsbelgium.mercator.feature.extraction.persistence",
    entityManagerFactoryRef = "featureExtractionEntityManager",
    transactionManagerRef = "featureExtractionTransactionManager"
)
public class FeatureExtractionConfiguration implements JmsConfig {

  @Bean
  @Primary
  @ConfigurationProperties(prefix = "spring.datasource")
  public DataSource featureExtractionDataSource() {
    return DataSourceBuilder.create().build();
  }

  @Bean
  @Primary
  public LocalContainerEntityManagerFactoryBean featureExtractionEntityManager() {
    LocalContainerEntityManagerFactoryBean em = new LocalContainerEntityManagerFactoryBean();
    DataSource ds = featureExtractionDataSource();
    em.setDataSource(ds);
    em.setPackagesToScan("be.dnsbelgium.mercator.feature.extraction.persistence");
    HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
    em.setJpaVendorAdapter(vendorAdapter);
    return em;
  }

  @Bean
  @Primary
  public PlatformTransactionManager featureExtractionTransactionManager() {
    JpaTransactionManager transactionManager = new JpaTransactionManager();
    transactionManager.setEntityManagerFactory(featureExtractionEntityManager().getObject());
    return transactionManager;
  }

}
