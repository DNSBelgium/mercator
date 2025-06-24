package be.dnsbelgium.mercator.vat.domain;

import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.slf4j.LoggerFactory.getLogger;

@SuppressWarnings({"ConstantConditions", "HttpUrlsUsage"})
public class UrlTests {

  private static final Logger logger = getLogger(UrlTests.class);

  // See https://square.github.io/okhttp/4.x/okhttp/okhttp3/-http-url/#why-another-url-model
  // for why use HttpUrl instead of java.net.URL

  @Test
  public void javaURL() throws MalformedURLException, URISyntaxException {
    String attack = "http://example.com/static/images/../../../../../etc/passwd";
    //noinspection deprecation
    
    logger.info("new URL(attack).getPath() = {}", URI.create(attack).getPath());
    logger.info("new URI(attack).getPath() = {}" , URI.create(attack).getPath());
    logger.info("HttpUrl.parse(attack).encodedPath() = {}", HttpUrl.parse(attack).encodedPath());
    logger.info("HttpUrl.parse(attack).pathSegments() = {}", HttpUrl.parse(attack).pathSegments());
    logger.info("HttpUrl.parse(attack).encodedPathSegments() = {}", HttpUrl.parse(attack).encodedPathSegments());
  }

  @Test
  public void equals() throws MalformedURLException {
    URL url1 = URI.create("http://example.com").toURL();
    URL url2 = URI.create("http://example.org").toURL();
    boolean equals = url1.equals(url2);
    logger.info("equals = {}", equals);

    HttpUrl httpUrl1 = HttpUrl.parse("http://example.com");
    HttpUrl httpUrl2 = HttpUrl.parse("http://example.org");
    logger.info("equals = {}", httpUrl1.equals(httpUrl2));

    httpUrl1 = HttpUrl.parse("http://example.com/abc/../def");
    httpUrl2 = HttpUrl.parse("http://example.com/def");
    logger.info("equals = {}", httpUrl1.equals(httpUrl2));

    url1 = URI.create("http://example.com/abc/../def").toURL();
    url2 = URI.create("http://example.org/def").toURL();
    logger.info("equals = {}", url1.equals(url2));

  }

}
