package be.dnsbelgium.mercator;

import be.dnsbelgium.mercator.tls.domain.TlsScanner;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.config.MeterFilter;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.batch.BatchAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.nio.file.Path;
import java.sql.*;


@ConfigurationPropertiesScan
@SpringBootApplication(scanBasePackages = {"be.dnsbelgium.mercator"} ,
exclude = {
        BatchAutoConfiguration.class,
        DataSourceAutoConfiguration.class,
})
@EnableBatchProcessing
public class MercatorApplication {

  // if we do this early enough, we don't have to set a system property when starting the JVM
  // (-Djava.security.properties=/path/to/custom/security.properties)
  static {
    TlsScanner.allowOldAlgorithms();
  }

  private static final Logger logger = LoggerFactory.getLogger(MercatorApplication.class);

  @SneakyThrows
  public static void runDuck(String query) {
    try (Connection conn = DriverManager.getConnection("jdbc:duckdb:");
         Statement stmt = conn.createStatement()) {
      //noinspection SqlSourceToSinkFlow
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

  public static void printMemoryLimits() {
    int mb = 1024 * 1024;
    MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
    long xmx = memoryBean.getHeapMemoryUsage().getMax() / mb;
    long xms = memoryBean.getHeapMemoryUsage().getInit() / mb;
    logger.info("Initial Memory (-Xms) = {} MB", xms);
    logger.info("Maximum Memory (-Xmx) = {} MB", xmx);
    logger.info("Use environment variable JAVA_TOOL_OPTIONS to adjust.");
    logger.info("For example:");
    logger.info("  docker run -e JAVA_TOOL_OPTIONS=\"-Xmx10G\" --rm  dnsbelgium/mercator");
  }



  public static void main(String[] args) throws IOException {
    printMemoryLimits();
    if (args.length > 0 && "duckdb".equals(args[0])) {
      if (args.length > 1) {
        runDuck(args[1]);
      } else {
        String query = new String(System.in.readAllBytes());
        runDuck(query);
      }
      System.exit(0);
    }
    logger.info("CWD = {}", Path.of("").toAbsolutePath());
    workAroundBatchMetricsBug();
    SpringApplication.run(MercatorApplication.class, args);
  }

  public static void workAroundBatchMetricsBug() {
    // Spring Batch has a minor bug that generates a warning at start-up.
    // see https://github.com/spring-projects/spring-batch/issues/4753
    // This snippet avoids the warning.
    Metrics.globalRegistry.config().meterFilter(MeterFilter.denyNameStartsWith("spring.batch.job.active"));
  }

}
