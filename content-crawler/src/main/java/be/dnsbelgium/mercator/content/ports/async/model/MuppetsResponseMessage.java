package be.dnsbelgium.mercator.content.ports.async.model;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper=false) // Remove warning from lombok
public class MuppetsResponseMessage extends ResponseMessage {
  private String id;
  private MuppetsRequestMessage request;
  private String hostname;
  private String url;
  private JsonNode errors;
  private String pageTitle;
  private String bucket;
  private String screenshotFile;
  private String htmlFile;
  private Integer htmlLength;
  private String harFile;
  private JsonNode metrics;
  private String ipv4;
  private String ipv6;
  private String browserVersion;
}
