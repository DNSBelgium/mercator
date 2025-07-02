package be.dnsbelgium.mercator.web.domain;

import okhttp3.HttpUrl;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@SuppressWarnings("HttpUrlsUsage")
class LinkPrioritizerTest {

  private static final Logger logger = getLogger(LinkPrioritizerTest.class);

  private final static LinkPrioritizer LINK_PRIORITIZER = new LinkPrioritizer();
  Link veryImportant   = new Link(HttpUrl.get("http://abc.be/algemene-voorwaarden"), "algemene voorwaarden");
  Link mediumImportant = new Link(HttpUrl.get("http://abc.be/algemene-voorwaarden"), "xyz");
  Link notImportant    = new Link(HttpUrl.get("http://abc.be/xyz"), "not important");

  @BeforeAll
  static void init() {
    LINK_PRIORITIZER.init();
  }

  @Test
  public void score() {
    Double scoreVeryImportant   = LINK_PRIORITIZER.computePriorityFor(veryImportant);
    Double scoreMediumImportant = LINK_PRIORITIZER.computePriorityFor(mediumImportant);
    Double scoreNotImportant    = LINK_PRIORITIZER.computePriorityFor(notImportant);
    logger.info("scoreVeryImportant   = {}", scoreVeryImportant);
    logger.info("scoreMediumImportant = {}", scoreMediumImportant);
    logger.info("scoreNotImportant    = {}", scoreNotImportant);
    assertThat(scoreVeryImportant)  .isBetween(0.0, 1.0);
    assertThat(scoreMediumImportant).isBetween(0.0, 1.0);
    assertThat(scoreNotImportant)   .isBetween(0.0, 1.0);
    assertThat(scoreMediumImportant).isBetween(scoreNotImportant, scoreVeryImportant);
  }

  @Test
  public void caseInsensitive() {
    HttpUrl url_mixed  = HttpUrl.get("http://abc.be/Algemene-Voorwaarden");
    HttpUrl url_lower  = HttpUrl.get("http://abc.be/Algemene-Voorwaarden".toLowerCase());
    HttpUrl url_upper  = HttpUrl.get("http://abc.be/Algemene-Voorwaarden".toUpperCase());
    String text = "ALGEMENE VOORWAARDEN";
    double prioMixedCase = LINK_PRIORITIZER.computePriorityFor(new Link(url_mixed, text));
    double prioLowerCase = LINK_PRIORITIZER.computePriorityFor(new Link(url_lower, text.toLowerCase()));
    double prioUpperCase = LINK_PRIORITIZER.computePriorityFor(new Link(url_upper, text.toUpperCase()));
    assertThat(prioLowerCase).isEqualTo(prioMixedCase);
    assertThat(prioUpperCase).isEqualTo(prioMixedCase);
  }

  @Test
  public void prioritize() {
    HttpUrl url = HttpUrl.get("http://www.example.com/algemene-voorwaarden");
    Link link = new Link(url, "cookies", HttpUrl.get("http://www.example.com/"));
    PrioritizedLink prioritizedLink = LINK_PRIORITIZER.prioritize(link);
    logger.info("prioritizedLink = {}", prioritizedLink.toString());
    assertThat(prioritizedLink.getReferer()).isEqualTo(link.getReferer());
    assertThat(prioritizedLink.getUrl()).isEqualTo(link.getUrl());
    assertThat(prioritizedLink).isEqualTo(link);

  }
}