package be.dnsbelgium.mercator;

import org.springframework.boot.SpringApplication;

public class TestMercatorApplication {

  public static void main(String[] args) {
    SpringApplication
            .from(MercatorApplication::main)
            .with(TestcontainersConfiguration.class)
            .run(args);
  }

}
