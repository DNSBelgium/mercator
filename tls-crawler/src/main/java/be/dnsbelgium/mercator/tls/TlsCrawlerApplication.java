package be.dnsbelgium.mercator.tls;

import be.dnsbelgium.mercator.tls.domain.TlsScanner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class TlsCrawlerApplication {

  public static void main(String[] args) {
    //  This method needs to be called as soon as possible after the start of the JVM
    TlsScanner.allowOldAlgorithms();
    SpringApplication.run(TlsCrawlerApplication.class, args);
  }

}
