package be.dnsbelgium.mercator;

import be.dnsbelgium.mercator.tls.domain.TlsScanner;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.SneakyThrows;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.io.IOException;
import java.sql.*;


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

  @SneakyThrows
  public static void runDuck(String query) {
    try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
         Statement stmt = conn.createStatement()) {
      if (!stmt.execute(query)) {
        return;
      }
      ResultSet rs = stmt.getResultSet();
      ObjectMapper mapper = new ObjectMapper();
      ResultSetMetaData meta = rs.getMetaData();
      int cols = meta.getColumnCount();

      while (rs.next()) {
        ObjectNode row = mapper.createObjectNode();
        for (int i = 1; i <= cols; i++) {
          row.putPOJO(meta.getColumnLabel(i), rs.getObject(i));
        }
        System.out.println(row.toString());
      }

    }
  }


  public static void main(String[] args) throws IOException {
    if (args.length > 0 && "duckdb".equals(args[0])) {
      if (args.length > 1) {
        runDuck(args[1]);
      } else {
        String query = new String(System.in.readAllBytes());
        runDuck(query);
      }
      System.exit(0);
    }

    SpringApplication.run(MercatorApplication.class, args);
  }



}
