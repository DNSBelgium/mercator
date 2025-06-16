package be.dnsbelgium.mercator.batch;

import lombok.Getter;
import lombok.ToString;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
@Getter
@ToString
public class BatchConfig {

  @Value("${mercator.input.file}")
  private String inputFile;

  @Value("${mercator.json.location}")
  private String outputDirectory;

  @Value("${mercator.shutdown-wait-seconds:0}")
  private int shutdownWaitSeconds;

  public Path outputPathFor(String jobName) {
    return Path.of(outputDirectory, jobName + ".json");
  }

  public Path outputDirectoryFor(String jobName) {
    return Path.of(outputDirectory, jobName);
  }
}
