package be.dnsbelgium.mercator.persistence;

import be.dnsbelgium.mercator.test.TestUtils;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonpCharacterEscapes;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import lombok.Data;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.simple.JdbcClient;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static be.dnsbelgium.mercator.common.SurrogateCodePoints.removeIncompleteSurrogates;
import static be.dnsbelgium.mercator.common.SurrogateCodePoints.replaceIncompleteSurrogates;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SurrogateCodeUnitsTest {

  @TempDir
  private File tempDir;
  private static final Logger logger = LoggerFactory.getLogger(SurrogateCodeUnitsTest.class);

  @Data
  public static class Person {
    String name;
  }

  @SuppressWarnings("ConstantValue")
  @Test
  public void malformedJSON() throws IOException {
    Person person = new Person();
    char c1 = 'a';
    char c2 = 55357;
    assertThat(Character.isHighSurrogate(c2)).isTrue();
    // a high surrogate code point should always be followed by a low surrogate code point
    // therefor, the following String will cause problems
    person.name =  "this is not ok: " + new String(new char[]{c1, c2});

    File file1 = writeAsJson("person.json", person);

    // Jackson can read it
    ObjectReader reader = TestUtils.jsonReader().reader();
    Person fromFile = reader.readValue(file1, Person.class);
    logger.info("fromFile = {}", fromFile);
    assertThat(fromFile.name).isEqualTo(person.name);

    // But duckdb cannot read it
    Exception thrown = assertThrows(UncategorizedSQLException.class, () -> readWithDuckDB(file1));
    logger.info("thrown = {}", thrown.getMessage());
    assertThat(thrown.getMessage()).contains("Invalid Input Error: Malformed JSON in file");

    // now fix the Person object
    person.name = replaceIncompleteSurrogates(person.name, "");
    File file2 = writeAsJson("person2.json", person);
    // now we can read it
    readWithDuckDB(file2);

    // or fix the JSON file
    // TODO: I have not yet found a decent way to fix the JSON file
  }

  private File writeAsJson(String fileName, Person person) throws IOException {
    File file = new File(tempDir, fileName);
    ObjectWriter writer = TestUtils.jsonWriter()
             // these features do not solve our issue
            .with(JsonGenerator.Feature.COMBINE_UNICODE_SURROGATES_IN_UTF8)
            .with(new JsonpCharacterEscapes());
    writer.writeValue(file, person);
    return file;
  }

  @SuppressWarnings("SqlSourceToSinkFlow")
  public void readWithDuckDB(File file) {
    JdbcClient client = JdbcClient.create(DuckDataSource.memory());
    String query = "select * from '%s' ".formatted(file.getAbsolutePath());
    List<Map<String, Object>> rows = client.sql(query).query().listOfRows();
    for (Map<String, Object> row : rows) {
      logger.info("row = {}", row);
    }
  }


  @Test
  public void testReplace() throws IOException {
    String input = "\uD83Dabc";
    System.out.println("input = " + input);
    String out = replaceIncompleteSurrogates(input, "x");
    System.out.println(input.length());
    System.out.println(out.length());
    assertThat(out).isEqualTo("xabc");

    String removed = removeIncompleteSurrogates(input);
    assertThat(removed).isEqualTo("abc");

    Person person = new Person();
    person.name = out;
    File outFile = new File(tempDir, "out.json");
    TestUtils.jsonWriter().writeValue(outFile, person);
    readWithDuckDB(outFile);

    person.name = input;
    File out2File = new File(tempDir, "out2.json");
    TestUtils.jsonWriter().writeValue(out2File, person);

    assertThrows(UncategorizedSQLException.class, () -> readWithDuckDB(out2File));
  }

}
