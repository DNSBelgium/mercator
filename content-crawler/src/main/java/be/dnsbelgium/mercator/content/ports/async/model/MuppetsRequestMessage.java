package be.dnsbelgium.mercator.content.ports.async.model;

import lombok.Data;

import java.util.UUID;

@Data
public class MuppetsRequestMessage implements RequestMessage {

  private String url;
  private UUID visitId;
  private String domainName;

  private String referer;

  private ScreenshotOptions screenshotOptions = new ScreenshotOptions();
  private BrowserOptions browserOptions = new BrowserOptions();

  private Boolean saveHtml = true;
  private Boolean saveScreenshot = false;
  private Boolean saveHar = true;
  private Integer retries = 0;

  @Data
  static class ScreenshotOptions {
    ScreenshotType type = ScreenshotType.webp;
    Integer quality;
    Boolean fullPage;
    BoundingBox clip;
    Boolean omitBackground;
    Encoding encoding;
  }

  enum ScreenshotType {
    png, jpeg, webp
  }

  @Data
  static class BoundingBox {
    Integer x;
    Integer y;
    Integer width;
    Integer height;
  }

  enum Encoding {
    base64, binary
  }

  @Data
  static class BrowserOptions {
    Boolean ignoreHTTPSErrors;
    Viewport defaultViewport;
    Integer slowMo;
  }

  @Data
  static class Viewport {
    Integer width;
    Integer height;
    Integer deviceScaleFactor;
    Boolean isMobile;
    Boolean hasTouch;
    Boolean isLandscape;
  }

}
