package be.dnsbelgium.mercator.vat.domain;

import be.dnsbelgium.mercator.common.VisitIdGenerator;
import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.test.TestUtils;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@SuppressWarnings("HttpUrlsUsage")
class PageTest {

  private static final Logger logger = getLogger(PageTest.class);

  @Test
  public void findLinks() {
    String responseBody = "<html><head><title>Hello world</title><body>Hi there</body></html>";
    Page page = makePageFrom(responseBody);
    logger.info("page = {}", page);
    logger.info("page.getLinks() = {}", page.getLinks());
    logger.info("page.getInnerLinks() = {}", page.getInnerLinks());
    assertThat(page.getLinks()).isEmpty();
    assertThat(page.getInnerLinks()).isEmpty();
  }

  @Test
  public void simplePage() throws IOException {
    String responseBody = getHtml("html/simple/index.html");
    Page page = makePageFrom(responseBody);
    logger.info("page = {}", page);
    logger.info("page.getLinks() = {}", page.getLinks());
    logger.info("page.getInnerLinks() = {}", page.getInnerLinks());
    assertThat(page.getLinks()).isEmpty();
    assertThat(page.getInnerLinks()).isEmpty();
  }

  @Test
  public void complexerPage() throws IOException {
    String responseBody = getHtml("html/complex/index.html");
    Page page = makePageFrom(responseBody);
    logger.info("page = {}", page);
    logger.info("page.getLinks() = {}", page.getLinks());
    logger.info("page.getInnerLinks() = {}", page.getInnerLinks());

    assertThat(page.getLinks().size()).isEqualTo(4);
    assertThat(page.getInnerLinks().size()).isEqualTo(2);

    HttpUrl referer = page.getUrl();

    Link expected1 = new Link(HttpUrl.get("http://www.example.com/complex/page-1"), "Page 1", referer);
    Link expected2 = new Link(HttpUrl.get("http://www.example.com/complex/page-2"), "Page 2", referer);
    Link expected3 = new Link(HttpUrl.get("http://www.google.be"), "External link", referer);
    Link pdf = new Link(HttpUrl.get("http://www.example.com/doc.pdf"), "Some pdf", referer);

    assertThat(page.getLinks()).contains(expected1);
    assertThat(page.getLinks()).contains(expected2);
    assertThat(page.getLinks()).contains(expected3);
    assertThat(page.getLinks()).contains(pdf);

    assertThat(page.getInnerLinks()).contains(expected1);
    assertThat(page.getInnerLinks()).contains(expected2);
  }

  @Test
  public void getSecondLevelDomainName() {
    assertThat(Page.getSecondLevelDomainName("http://www.example.com")).isEqualTo("example.com");
    assertThat(Page.getSecondLevelDomainName("https://www.1.2.3.example.com")).isEqualTo("example.com");
    assertThat(Page.getSecondLevelDomainName("https://www.1.2.3.example.com/abc/d?q=45")).isEqualTo("example.com");
    assertThat(Page.getSecondLevelDomainName("https://10.20.30.40/abc/def?q=45")).isEqualTo("30.40");
    assertThat(Page.getSecondLevelDomainName("")).isEqualTo("");
    assertThat(Page.getSecondLevelDomainName("../xyz/doc.pdf")).isEqualTo("");
    assertThat(Page.getSecondLevelDomainName("file:///xyz/doc.pdf")).isEqualTo("");
  }

  private Page makePageFrom(String responseBody) {
    HttpUrl url = HttpUrl.get("http://www.example.com/");
    MediaType mediaType = MediaType.parse("text/html");
    return new Page(url, TestUtils.now(), TestUtils.now().plusMillis(120), 200, responseBody, -1, mediaType, null);
  }

  private String getHtml(String resourcePath) throws IOException {
    byte[] bytes = FileCopyUtils.copyToByteArray(new ClassPathResource(resourcePath).getInputStream());
    return new String(bytes);
  }

  @Test
  public void asPageVisit() {
    var visitId = VisitIdGenerator.generate();
    VisitRequest visitRequest = new VisitRequest(visitId, "abc.be");
    String responseBody = "<html><body><h1>Hello world</h1></body></html>";
    Page page = makePageFrom(responseBody);
    PageVisit pageVisit = page.asPageVisit(visitRequest);
    logger.info("pageVisit = {}", pageVisit);
    assertThat(pageVisit.getVisitId()).isEqualTo(visitRequest.getVisitId());
    assertThat(pageVisit.getDomainName()).isEqualTo("abc.be");
    assertThat(pageVisit.getCrawlStarted()).isEqualTo(page.getVisitStarted());
    assertThat(pageVisit.getCrawlFinished()).isEqualTo(page.getVisitFinished());
    assertThat(pageVisit.getStatusCode()).isEqualTo(page.getStatusCode());
    assertThat(pageVisit.getResponseBody()).isEqualTo(responseBody);
  }

  @Test
  public void testURL() throws MalformedURLException, URISyntaxException {
    String urlString = "http://www.allmatte.be/contact/#header-newsletter-signup";
    URL url = new URI(urlString).toURL();
    logger.info("urlString = {}", urlString);
    logger.info("url       = {}", url);

    logger.info("url.getAuthority   = {}", url.getAuthority());
    logger.info("url.getHost        = {}", url.getHost());
    logger.info("url.getPath        = {}", url.getPath());
    logger.info("url.getProtocol    = {}", url.getProtocol());
    logger.info("url.getQuery       = {}", url.getQuery());
    logger.info("url.getRef         = {}", url.getRef());
    logger.info("url.getUserInfo    = {}", url.getUserInfo());
  }

  @Test
  public void skipBinaryData() {
    Page page = makePageFrom("This is binary data with a null \u0000 byte");
    VisitRequest visitRequest = new VisitRequest("test.be");
    PageVisit pageVisit = page.asPageVisit(visitRequest);
    logger.info("pageVisit.getResponseBody = {}", pageVisit.getResponseBody());
    assertThat(pageVisit.getResponseBody()).isNull();
  }

  @Test
  public void ignoreInvalidLinks() {
    String validLink   = "http://café-" + StringUtils.repeat("a", 25) + ".be";
    String invalidLink = "http://café-" + StringUtils.repeat("a", 256) + ".be";
    String html = String.format(
        "<html><body>" +
            "<a href='%s'>valid link</a>" +
            "<a href='%s'>invalid link</a>" +
        "</body></html>", validLink, invalidLink);
    logger.info("html = {}", html);
    Page page = makePageFrom(html);
    logger.info("page.getLinks() = {}", page.getLinks());
    assertThat(page.getLinks()).hasSize(1);
    String expected = page.getLinks().stream().map(link -> link.getUrl().host()).toList().get(0);
    assertThat(expected).isEqualTo("xn--caf-aaaaaaaaaaaaaaaaaaaaaaaaa-duc.be");
  }
}