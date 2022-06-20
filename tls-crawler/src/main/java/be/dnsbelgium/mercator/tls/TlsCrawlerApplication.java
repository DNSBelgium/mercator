package be.dnsbelgium.mercator.tls;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.security.Security;

@SpringBootApplication
public class TlsCrawlerApplication {

  public static void main(String[] args) {
    System.out.println("Starting TlsCrawlerApplication");
    System.out.println("setting security property \"jdk.tls.disabledAlgorithms\" to \"NULL\"");
    Security.setProperty("jdk.tls.disabledAlgorithms", "NULL");
    SpringApplication.run(TlsCrawlerApplication.class, args);
  }

}
