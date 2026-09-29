package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.test.TestUtils;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.simple.JdbcClient;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.util.List;
import java.util.Map;

import static be.dnsbelgium.mercator.common.SurrogateCodePoints.replaceIncompleteSurrogates;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Regression tests for the interaction between crawled web data containing an incomplete
 * surrogate pair and DuckDB's {@code read_json}.
 *
 * <p>Web pages can contain a lone (unpaired) UTF-16 surrogate. When such a String is serialized
 * with the production Jackson 3 ({@code tools.jackson}) mapper, Jackson escapes it as
 * {@code \\uD83D}. That is syntactically valid JSON, but DuckDB still rejects the file because,
 * when it decodes the escape, it finds a high surrogate with no matching low surrogate
 * ("no low surrogate in string"). {@code SurrogateCodePoints.replaceIncompleteSurrogates}
 * removes these incomplete surrogates so the JSON becomes readable by DuckDB.
 *
 * <p>These tests use the exact Jackson 3 mapper used in production (via {@link TestUtils}); they
 * do not use Jackson 2.
 */
public class SurrogateCodeUnitsTest {

  @TempDir
  private File tempDir;
  private static final Logger logger = LoggerFactory.getLogger(SurrogateCodeUnitsTest.class);

  // The exact Jackson 3 mapper used by the pipeline in production.
  private static final ObjectMapper jackson3 = TestUtils.jsonReader();

  @Data
  public static class Person {
    String name;
  }

  /** Builds a String that mimics crawled web data ending in a lone (unpaired) high surrogate. */
  private static String webDataWithIncompleteSurrogate() {
    char highSurrogate = 55357; // 0xD83D, a high surrogate with no following low surrogate
      //noinspection ConstantValue
      assertThat(Character.isHighSurrogate(highSurrogate)).isTrue();
    return "invalid web data: " + new String(new char[]{'a', highSurrogate});
  }

  /**
   * (a) Proves that invalid web data (an incomplete surrogate pair) trips DuckDB when
   * {@code replaceIncompleteSurrogates} is NOT applied: Jackson 3 serializes it to a
   * {@code \\uD83D} escape and DuckDB fails to read the resulting file.
   */
  @Test
  public void incompleteSurrogateTripsDuckDb() {
    Person person = new Person();
    person.name = webDataWithIncompleteSurrogate();

    File file = writeAsJson("unsanitized.json", person);

    assertThatThrownBy(() -> readWithDuckDB(file))
        .isInstanceOf(UncategorizedSQLException.class)
        .hasMessageContaining("Malformed JSON in file")
        .hasMessageContaining("no low surrogate in string");
  }

  /**
   * (b) Proves that {@code replaceIncompleteSurrogates} fixes the issue: after sanitizing the same
   * web data, Jackson 3 produces JSON that DuckDB reads back successfully, with the incomplete
   * surrogate removed.
   */
  @Test
  public void replaceIncompleteSurrogatesFixesDuckDbRead() {
    Person person = new Person();
    person.name = replaceIncompleteSurrogates(webDataWithIncompleteSurrogate(), "");

    File file = writeAsJson("sanitized.json", person);

    List<Map<String, Object>> rows = readWithDuckDB(file);
    assertThat(rows).hasSize(1);
    assertThat(rows.getFirst().get("name")).isEqualTo("invalid web data: a");
  }

  private File writeAsJson(String fileName, Person person) {
    File file = new File(tempDir, fileName);
    jackson3.writeValue(file, person);
    return file;
  }

  private List<Map<String, Object>> readWithDuckDB(File file) {
    JdbcClient client = JdbcClient.create(DuckDataSource.memory());
    String query = "select * from '%s' ".formatted(file.getAbsolutePath());
    List<Map<String, Object>> rows = client.sql(query).query().listOfRows();
    for (Map<String, Object> row : rows) {
      logger.info("row = {}", row);
    }
    return rows;
  }
}
