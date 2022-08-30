package be.dnsbelgium.mercator.content.ports.async.model;

import java.util.UUID;

class ResolveContentResponseMessageTest {

  public static MuppetsResponseMessage resolveContentResponseMessage() {
    MuppetsResponseMessage muppetsResponseMessage = new MuppetsResponseMessage();
    muppetsResponseMessage.setBrowserVersion("Chrome 149553");
    muppetsResponseMessage.setBucket("MyBucket");
    muppetsResponseMessage.setErrors(null);
    muppetsResponseMessage.setHostname("dnsbelgium.be");
    muppetsResponseMessage.setHarFile("/tmp/har.file");
    muppetsResponseMessage.setHtmlFile("/tmp/index.html");
    muppetsResponseMessage.setScreenshotFile("/tmp/screenshot.png");
    muppetsResponseMessage.setHtmlLength(999);
    muppetsResponseMessage.setId(UUID.randomUUID().toString());
    muppetsResponseMessage.setIpv4("1.2.3.4");
    muppetsResponseMessage.setIpv6("::1");
    muppetsResponseMessage.setMetrics(null);
    muppetsResponseMessage.setPageTitle("My super website");
    muppetsResponseMessage.setUrl("https://www.dnsbelgium.be");
    muppetsResponseMessage.setRequest(new MuppetsRequestMessage());

    return muppetsResponseMessage;
  }
}
