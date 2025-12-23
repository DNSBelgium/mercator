package be.dnsbelgium.mercator.schedule;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "log.shipping")
@Data
public class LogShippingSettings {

  private boolean enabled = false;
  private String glob_pattern;
  private int maxFailures = -1;
  private String interval = "60s";

  private final S3 s3 = new S3();

  public S3 s3() {
    return s3;
  }

  @Data
  public static class S3 {
    private String bucketName;
    private String accessKey;
    private String secretKey;
    private String endpoint;
    private String region;
  }

}
