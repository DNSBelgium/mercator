package be.dnsbelgium.mercator.web.domain;

import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class LinkTest {

  private static final Logger logger = getLogger(LinkTest.class);

  @Test
  public void clean() {
    HttpUrl url      = HttpUrl.get("https://www.ranson.be/contact#");
    HttpUrl expected = HttpUrl.get("https://www.ranson.be/contact");
    HttpUrl clean    = new Link(url, "whatever").getUrl();
    logger.info("clean = {}", clean);
    logger.info("url.encodedPath = {}",   url.encodedPath());
    logger.info("clean.encodedPath = {}", clean.encodedPath());
    assertThat(clean).isEqualTo(expected);
  }
}