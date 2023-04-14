package be.dnsbelgium.mercator.content.domain.content;

import be.dnsbelgium.mercator.content.dto.MuppetsResolution;

import java.util.UUID;

public class ContentResolutionTest {

  // Object Mothers
  public static MuppetsResolution contentResolutionTestHtmlFailTooBig() {
    return new MuppetsResolution(UUID.randomUUID(), "dns.be", "https://www.dns.be", true,
            "uploading to S3 cancelled, html size bigger then 10Mb", "https://www.dnsbelgium.be", "MyBucket",
            "screenshot.png", "index.html", 10, "file.har", "{}",
            "1.2.3.4", "::0", "BlaBla 1.2", 1);
  }
  public static MuppetsResolution contentResolutionTestScreenshotFailTooBig() {
    return new MuppetsResolution(UUID.randomUUID(), "dns.be", "https://www.dns.be", true,
            "screenshot bigger then 10MiB Upload to S3 cancelled", "https://www.dnsbelgium.be", "MyBucket",
            "screenshot.png", "index.html", 10, "file.har", "{}",
            "1.2.3.4", "::0", "BlaBla 1.2", 1);
  }
  public static MuppetsResolution contentResolutionTestHtmlUploadFail() {
    return new MuppetsResolution(UUID.randomUUID(), "dns.be", "https://www.dns.be", true,
            "Upload failed for html file", "https://www.dnsbelgium.be", "MyBucket",
            "screenshot.png", "index.html", 10, "file.har", "{}",
            "1.2.3.4", "::0", "BlaBla 1.2", 1);
  }
  public static MuppetsResolution contentResolutionTestScreenshotUploadFail() {
    return new MuppetsResolution(UUID.randomUUID(), "dns.be", "https://www.dns.be", true,
            "Upload failed for html file", "https://www.dnsbelgium.be", "MyBucket",
            "screenshot.png", "index.html", 10, "file.har", "{}",
            "1.2.3.4", "::0", "BlaBla 1.2", 1);
  }
  public static MuppetsResolution contentResolutionTestNameNotResolved() {
    return new MuppetsResolution(UUID.randomUUID(), "dns.be", "https://www.dns.be", true,
            "net::ERR_NAME_NOT_RESOLVED", "https://www.dnsbelgium.be", "MyBucket",
            "screenshot.png", "index.html", 10, "file.har", "{}",
            "1.2.3.4", "::0", "BlaBla 1.2", 1);
  }
  public static MuppetsResolution contentResolutionTestTimeOut() {
    return new MuppetsResolution(UUID.randomUUID(), "dns.be", "https://www.dns.be", true,
            "Navigation timeout of 15000 ms exceeded", "https://www.dnsbelgium.be", "MyBucket",
            "screenshot.png", "index.html", 10, "file.har", "{}",
            "1.2.3.4", "::0", "BlaBla 1.2", 1);
  }
  public static MuppetsResolution contentResolutionTest() {
    return new MuppetsResolution(UUID.randomUUID(), "dns.be", "https://www.dns.be", true,
                                 null, "https://www.dnsbelgium.be", "MyBucket",
                                 "screenshot.png", "index.html", 10, "file.har", "{}",
                                 "1.2.3.4", "::0", "BlaBla 1.2", 1);
  }
}
