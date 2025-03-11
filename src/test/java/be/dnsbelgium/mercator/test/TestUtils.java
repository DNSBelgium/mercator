package be.dnsbelgium.mercator.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

public class TestUtils {

  // a clock that ticks only once per microsecond
  private static final Clock microsecondClock = Clock.tick(Clock.systemUTC(), Duration.ofNanos(1000));

  private final static ObjectMapper objectMapper = new ObjectMapper();
  static {
    objectMapper.registerModule(new Jdk8Module());
    objectMapper.registerModule(new JavaTimeModule());
    objectMapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
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
   * An ObjectWriter to be used in tests.
   * Alternative would be to inject the one from the ApplicationContext but that would force many tests
   * to be a @SpringBootTest which is usually slower.
   * <p>
   * One caveat is that we PropertyNamingStrategy should be the same as the one used by the production code.
   * </p>
   * <p>
   *   If this comes to bite us too often, we should remove this method and inject an ObjectWriter in the tests.
   * </p>
   * @return an ObjectWriter to be used in tests
   */
  public static ObjectWriter jsonWriter() {
    return objectMapper.writerWithDefaultPrettyPrinter();
  }

  public static ObjectMapper jsonReader() {
    return objectMapper;
  }

}
