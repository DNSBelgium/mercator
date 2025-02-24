package be.dnsbelgium.mercator;

import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = {"be.dnsbelgium.mercator"} ,
exclude = {
        BatchAutoConfiguration.class,
})
@EnableBatchProcessing
//@Profile("web")
public class MercatorApplication {

  public static void main(String[] args) {
    SpringApplication.run(MercatorApplication.class, args);
  }



}
