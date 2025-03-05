package be.dnsbelgium.mercator.test;

import be.dnsbelgium.mercator.feature.extraction.HtmlFeatureExtractor;
import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeatures;
import be.dnsbelgium.mercator.vat.domain.PageVisit;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;

public class ObjectMother {

  private final HtmlFeatureExtractor htmlFeatureExtractor = new HtmlFeatureExtractor(new SimpleMeterRegistry(), false);

  Instant started  = LocalDateTime.of(2024,11,28, 23, 59).toInstant(ZoneOffset.UTC);

  public PageVisit pageVisit1() {
    return PageVisit.builder()
            .visitId("v104")
            .html("<h1>I am a page </h1>")
            .url("https://www.dnsbelgium.be/")
            .bodyText("I am a page")
            .domainName("dnsbelgium.be")
            .statusCode(200)
            .path("/")
            .build();
  }

  public PageVisit pageVisit2() {
    return PageVisit.builder()
            .visitId("v104")
            .html("<h1>I am the EN page </h1>")
            .url("https://www.dnsbelgium.be/en")
            .bodyText("I am the EN page")
            .domainName("dnsbelgium.be")
            .statusCode(200)
            .path("/en")
            .build();
  }

  public PageVisit pageVisit3() {
    return PageVisit.builder()
            .visitId("v104")
            .html("<h1>I am the contact page </h1>")
            .url("https://www.dnsbelgium.be/en/contact")
            .bodyText("I am the contact page")
            .domainName("dnsbelgium.be")
            .vatValues(List.of("BE0466158640", "BE0841242495"))
            .statusCode(200)
            .path("/en/contact")
            .build();
  }

  public PageVisit pageVisit4() {
    return PageVisit.builder()
            .visitId("v105")
            .html(null)
            .url("https://www.no-website.org/")
            .bodyText(null)
            .domainName("no-website.org")
            .statusCode(400)
            .path("/")
            .build();
  }

  public HtmlFeatures htmlFeatures1() {
    return htmlFeatureExtractor
            .extractFromHtml(
                    "<h1>I am a page </h1>",
                    "https://www.dnsbelgium.be/",
                    "dnsbelgium.be");
  }

  public HtmlFeatures htmlFeatures2() {
    return htmlFeatureExtractor
            .extractFromHtml(
                    "<h1>I am the English page </h1>",
                    "https://www.dnsbelgium.be/en",
                    "dnsbelgium.be");
  }

  public HtmlFeatures htmlFeatures3() {
    return htmlFeatureExtractor
            .extractFromHtml(
                    "<h1>I am the Contact page </h1>",
                    "https://www.dnsbelgium.be/en/contact",
                    "dnsbelgium.be");
  }

  public WebCrawlResult webCrawlResult1() {
    return WebCrawlResult.builder()
            .crawlFinished(started)
            .crawlFinished(started.plusMillis(235))
            .domainName("dnsbelgium.be")
            .matchingUrl("https://www.dnsbelgium.be/en/contact")
            .vatValues(List.of("BE0466158640", "BE0841242495"))
            .visitedUrls(List.of("https://www.dnsbelgium.be/", "https://www.dnsbelgium.be/en","https://www.dnsbelgium.be/en/contact"))
            .startUrl("https://www.dnsbelgium.be/")
            .htmlFeatures(List.of(htmlFeatures1(), htmlFeatures2(), htmlFeatures3()))
            .pageVisits(List.of(pageVisit1(), pageVisit2(), pageVisit3()))
            .build();

  }

  public WebCrawlResult webCrawlResult2() {
    return WebCrawlResult.builder()
            .crawlFinished(started.plusMillis(10))
            .crawlFinished(started.plusSeconds(115))
            .domainName("no-website.org")
            .visitedUrls(List.of())
            .startUrl("https://www.no-website.be/")
            .htmlFeatures(List.of())
            .pageVisits(List.of(pageVisit4()))
            .build();
  }
}
