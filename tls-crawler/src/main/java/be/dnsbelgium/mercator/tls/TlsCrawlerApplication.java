package be.dnsbelgium.mercator.tls;

import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.Security;

import static org.slf4j.LoggerFactory.getLogger;

@SpringBootApplication
public class TlsCrawlerApplication {

  private static final Logger logger = getLogger(TlsCrawlerApplication.class);

  public static void main(String[] args) {
    logger.info("setting security property \"jdk.tls.disabledAlgorithms\" to \"NULL\"");
    Security.setProperty("jdk.tls.disabledAlgorithms", "NULL");
    SpringApplication.run(TlsCrawlerApplication.class, args);
  }

}
