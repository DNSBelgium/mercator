package be.dnsbelgium.mercator.web.domain;

import be.dnsbelgium.mercator.test.ObjectMother;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@SuppressWarnings("HttpUrlsUsage")
class VatLinkPrioritizerTest {

  private static final Logger logger = getLogger(VatLinkPrioritizerTest.class);

  private final static VatLinkPrioritizer LINK_PRIORITIZER = new VatLinkPrioritizer();
  Link veryImportant   = new Link(HttpUrl.get("http://abc.be/algemene-voorwaarden"), "algemene voorwaarden");
  Link mediumImportant = new Link(HttpUrl.get("http://abc.be/algemene-voorwaarden"), "xyz");
  Link notImportant    = new Link(HttpUrl.get("http://abc.be/xyz"), "not important");

  @BeforeAll
  static void init() {
    LINK_PRIORITIZER.init();
  }

  @Test
  public void score() {
    var url = HttpUrl.get("http://abc.be/");
    SiteVisit siteVisit = new SiteVisit(url);
    Page currentPage = new ObjectMother().page1();
    Double scoreVeryImportant   = LINK_PRIORITIZER.computePriorityFor(siteVisit, currentPage, veryImportant);
    Double scoreMediumImportant = LINK_PRIORITIZER.computePriorityFor(siteVisit, currentPage, mediumImportant);
    Double scoreNotImportant    = LINK_PRIORITIZER.computePriorityFor(siteVisit, currentPage, notImportant);
    logger.info("scoreVeryImportant   = {}", scoreVeryImportant);
    logger.info("scoreMediumImportant = {}", scoreMediumImportant);
    logger.info("scoreNotImportant    = {}", scoreNotImportant);
    assertThat(scoreVeryImportant)  .isBetween(0.0, 1.0);
    assertThat(scoreMediumImportant).isBetween(0.0, 1.0);
    assertThat(scoreNotImportant)   .isBetween(0.0, 1.0);
    assertThat(scoreMediumImportant).isBetween(scoreNotImportant, scoreVeryImportant);
  }

  @Test
  public void referrer() {
    Link veryImportant   = new Link(HttpUrl.get("http://abc.be/algemene-voorwaarden"), "algemene voorwaarden", HttpUrl.get("http://abc.be/"));
    System.out.println("veryImportant = " + veryImportant);
    System.out.println("veryImportant = " + veryImportant.getReferer());
  }


  @Test
  public void caseInsensitive() {
    HttpUrl url_mixed  = HttpUrl.get("http://abc.be/Algemene-Voorwaarden");
    HttpUrl url_lower  = HttpUrl.get("http://abc.be/Algemene-Voorwaarden".toLowerCase());
    HttpUrl url_upper  = HttpUrl.get("http://abc.be/Algemene-Voorwaarden".toUpperCase());
    String text = "ALGEMENE VOORWAARDEN";
    SiteVisit siteVisit = new SiteVisit(HttpUrl.get("http://abc.be/"));
    Page currentPage = new ObjectMother().page1();
    double prioMixedCase = LINK_PRIORITIZER.computePriorityFor(siteVisit, currentPage, new Link(url_mixed, text));
    double prioLowerCase = LINK_PRIORITIZER.computePriorityFor(siteVisit, currentPage, new Link(url_lower, text.toLowerCase()));
    double prioUpperCase = LINK_PRIORITIZER.computePriorityFor(siteVisit, currentPage, new Link(url_upper, text.toUpperCase()));
    assertThat(prioLowerCase).isEqualTo(prioMixedCase);
    assertThat(prioUpperCase).isEqualTo(prioMixedCase);
  }

  @Test
  public void prioritize() {
    HttpUrl url = HttpUrl.get("http://www.example.com/algemene-voorwaarden");
    Link link = new Link(url, "cookies", HttpUrl.get("http://www.example.com/"));
    SiteVisit siteVisit = new SiteVisit(HttpUrl.get("http://abc.be/"));
    Page page = new ObjectMother().page1();
    PrioritizedLink prioritizedLink = LINK_PRIORITIZER.prioritize(siteVisit, page, link);
    logger.info("prioritizedLink = {}", prioritizedLink.toString());
    assertThat(prioritizedLink.getReferer()).isEqualTo(link.getReferer());
    assertThat(prioritizedLink.getUrl()).isEqualTo(link.getUrl());
    assertThat(prioritizedLink).isEqualTo(link);

  }
}