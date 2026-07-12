package be.dnsbelgium.mercator.test;

import be.dnsbelgium.mercator.batch.JsonConfiguration;
import be.dnsbelgium.mercator.persistence.JdbcClientFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.json.JsonMapper;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class TestUtils {

  // a clock that ticks only once per microsecond
  private static final Clock microsecondClock = Clock.tick(Clock.systemUTC(), Duration.ofNanos(1000));

  private final static JsonMapper mapper;

  static {
    JsonConfiguration configuration = new JsonConfiguration();
    mapper = configuration.mapper();
  }

  /**
   * Useful in tests because some operating systems support nanosecond precision
   * while others only support microsecond precision.
   * <p>
   * For example, when saving a Timestamp with nanosecond precision to duckdb and retrieving it again
   * we lose the last 3 digits (value is truncated to microseconds) which does not cause unit tests to fail on macOS
   * (because the JVM also truncates to microseconds) but it does cause failures on Ubuntu (in GitHub actions).
   *
   * @return The current instant, with microsecond precision.
   */
  public static Instant now() {
    return Instant.now(microsecondClock);
  }

  /**
   * The ObjectMapper used is created by the method that also produces the @Bean that is used in production code.
   * This construct allows us to use an ObjectWriter and ObjectMapper in Tests that do not load a Spring context
   *
   * @return an ObjectWriter to be used in tests
   */
  public static ObjectWriter jsonWriter() {
    return mapper.writerWithDefaultPrettyPrinter();
  }

  public static ObjectMapper jsonReader() {
    return mapper;
  }

  public static JdbcClientFactory jdbcClientFactory() {
    return new JdbcClientFactory();
  }
}
