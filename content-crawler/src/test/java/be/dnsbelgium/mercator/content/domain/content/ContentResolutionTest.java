package be.dnsbelgium.mercator.content.domain.content;

import be.dnsbelgium.mercator.content.dto.MuppetsResolution;

import java.util.UUID;

public class ContentResolutionTest {

  // Object Mothers

  public static MuppetsResolution contentResolutionTest() {
    return new MuppetsResolution(UUID.randomUUID(), "dns.be", "https://www.dns.be", true,
                                 null, "https://www.dnsbelgium.be", "MyBucket",
                                 "screenshot.png", "index.html", 10, "file.har", "{}",
                                 "1.2.3.4", "::0", "BlaBla 1.2", 1);
  }
}
