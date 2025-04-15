package be.dnsbelgium.mercator;

import be.dnsbelgium.mercator.tls.domain.TlsScanner;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;


@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = {"be.dnsbelgium.mercator"} ,
exclude = {
        BatchAutoConfiguration.class,
})
@EnableBatchProcessing
public class MercatorApplication {

  // if we do this early enough, we don't have to set a system property when starting the JVM
  // (-Djava.security.properties=/path/to/custom/security.properties)
  static {
    TlsScanner.allowOldAlgorithms();
  }

  public static void main(String[] args) {
    SpringApplication.run(MercatorApplication.class, args);
  }



}
