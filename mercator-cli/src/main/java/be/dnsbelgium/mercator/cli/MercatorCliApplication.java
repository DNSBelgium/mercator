package be.dnsbelgium.mercator.cli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MercatorCliApplication {

  /*
     For best UX it is best to run this in a real Terminal instead of an IDEA Terminal.
     => better command completion etc

   */

  public static void main(String[] args) {
    SpringApplication.run(MercatorCliApplication.class, args);
  }


}
