package be.dnsbelgium.mercator.web.domain;

import lombok.Data;
import okhttp3.HttpUrl;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

@Data
public class Link {

  private final HttpUrl url;
  private final String text;

  private static final Logger logger = getLogger(Link.class);

  // TODO: referer could also be a Link instead of HttpUrl => would allow us to get the path that was followed
  private HttpUrl referer;

  public Link(HttpUrl url, String text, HttpUrl referer) {
    this.url = clean(url);
    this.text = text;
    this.referer = referer;
  }

  public Link(HttpUrl url, String text) {
    this(url, text, null);
  }

  private HttpUrl clean(HttpUrl url) {
    // create a new url with the query and fragment removed
    try {
      return new HttpUrl.Builder()
          .scheme(url.scheme())
          .host(url.host())
          .port(url.port())
          .encodedPath(url.encodedPath())
          .build();
    } catch (IllegalArgumentException e) {
      logger.info("Invalid HttpUrl: {}: {}", url, e.getMessage());
      throw e;
    }
  }

  public Link(Link link) {
    this.url  = link.url;
    this.text = link.text;
    this.referer = link.referer;
  }
}
