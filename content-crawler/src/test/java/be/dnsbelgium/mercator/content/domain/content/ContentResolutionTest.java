package be.dnsbelgium.mercator.content.domain.content;

import be.dnsbelgium.mercator.content.dto.MuppetsResolution;

import java.util.UUID;

public class ContentResolutionTest {

  // Object Mothers
  public static MuppetsResolution contentResolutionTestHtmlFailTooBig() {
    return new MuppetsResolution(UUID.randomUUID(), "dns.be", "https://www.dns.be", true,
            null, "https://www.dnsbelgium.be", "MyBucket",
            "screenshot.png", "index.html", 105000000, "file.har", "{}",
            "1.2.3.4", "::0", "BlaBla 1.2", 1, true,false,false);
  }
  public static MuppetsResolution contentResolutionTestScreenshotFailTooBig() {
    return new MuppetsResolution(UUID.randomUUID(), "dns.be", "https://www.dns.be", true,
            null, "https://www.dnsbelgium.be", "MyBucket",
            "screenshot.png", "index.html", 10, "file.har", "{}",
            "1.2.3.4", "::0", "BlaBla 1.2", 1,false,true,false);
  }
  public static MuppetsResolution contentResolutionTestHtmlUploadFail() {
    return new MuppetsResolution(UUID.randomUUID(), "dns.be", "https://www.dns.be", false,
            "Upload failed for html file", "https://www.dnsbelgium.be", "MyBucket",
            "screenshot.png", "index.html", 10, "file.har", "{}",
            "1.2.3.4", "::0", "BlaBla 1.2", 1,false,false,false);
  }
  public static MuppetsResolution contentResolutionTestScreenshotUploadFail() {
    return new MuppetsResolution(UUID.randomUUID(), "dns.be", "https://www.dns.be", false,
            "Upload failed for screenshot file", "https://www.dnsbelgium.be", "MyBucket",
            "screenshot.png", "index.html", 10, "file.har", "{}",
            "1.2.3.4", "::0", "BlaBla 1.2", 1,false,false,false);
  }
  public static MuppetsResolution contentResolutionTestNameNotResolved() {
    return new MuppetsResolution(UUID.randomUUID(), "dns.be", "https://www.dns.be", false,
            "net::ERR_NAME_NOT_RESOLVED", "https://www.dnsbelgium.be", "MyBucket",
            "screenshot.png", "index.html", 10, "file.har", "{}",
            "1.2.3.4", "::0", "BlaBla 1.2", 1,false,false,false);
  }
  public static MuppetsResolution contentResolutionTestTimeOut() {
    return new MuppetsResolution(UUID.randomUUID(), "dns.be", "https://www.dns.be", false,
            "Navigation timeout of 15000 ms exceeded", "https://www.dnsbelgium.be", "MyBucket",
            "screenshot.png", "index.html", 10, "file.har", "{}",
            "1.2.3.4", "::0", "BlaBla 1.2", 1,false,false,false);
  }

  public static MuppetsResolution contentResolutionTestUnexpectedError() {
    return new MuppetsResolution(UUID.randomUUID(), "dns.be", "https://www.dns.be", false,
            "UnexpectedError occured", "https://www.dnsbelgium.be", "MyBucket",
            "screenshot.png", "index.html", 10, "file.har", "{}",
            "1.2.3.4", "::0", "BlaBla 1.2", 1,false,false,false);
  }
  public static MuppetsResolution contentResolutionTest() {
    return new MuppetsResolution(UUID.randomUUID(), "dns.be", "https://www.dns.be", true,
                                 null, "https://www.dnsbelgium.be", "MyBucket",
                                 "screenshot.png", "index.html", 10, "file.har", "{}",
                                 "1.2.3.4", "::0", "BlaBla 1.2", 1,false,false,false);
  }
}
