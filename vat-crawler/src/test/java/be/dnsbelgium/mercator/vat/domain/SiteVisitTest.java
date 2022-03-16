package be.dnsbelgium.mercator.vat.domain;

import okhttp3.HttpUrl;
import org.apache.commons.lang3.RandomStringUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

class SiteVisitTest {

  private static final Logger logger = getLogger(SiteVisitTest.class);

  @Test
  public void getHighestPriorityLink() {
    HttpUrl url = HttpUrl.get("http://localhost");
    SiteVisit visit = new SiteVisit(url);
    PrioritizedLink link1 = makeRandomLinkWithPriority(1.0);
    PrioritizedLink link2 = makeRandomLinkWithPriority(0.5);
    PrioritizedLink link3 = makeRandomLinkWithPriority(0.2);
    PrioritizedLink link4 = makeRandomLinkWithPriority(0.0);

    Optional<PrioritizedLink> highPrioLink = visit.getHighestPriorityLink();
    assertThat(highPrioLink).isEmpty();

    visit.addLinkToVisit(link4);
    highPrioLink = visit.getHighestPriorityLink();
    logger.info("after adding l4: highPrioLink = {}", highPrioLink);
    assertThat(highPrioLink).isPresent();
    assertThat(highPrioLink.get()).isEqualTo(link4);

    visit.addLinkToVisit(link2);
    highPrioLink = visit.getHighestPriorityLink();
    logger.info("after adding l2: highPrioLink = {}", highPrioLink);
    assertThat(highPrioLink).isPresent();
    assertThat(highPrioLink.get()).isEqualTo(link2);

    visit.addLinkToVisit(link1);
    highPrioLink = visit.getHighestPriorityLink();
    logger.info("after adding l1: highPrioLink = {}", highPrioLink);
    assertThat(highPrioLink).isPresent();
    assertThat(highPrioLink.get()).isEqualTo(link1);

    visit.addLinkToVisit(link3);
    highPrioLink = visit.getHighestPriorityLink();
    logger.info("after adding l3: highPrioLink = {}", highPrioLink);
    assertThat(highPrioLink).isPresent();
    assertThat(highPrioLink.get()).isEqualTo(link1);

    visit.markVisited(link1);
    highPrioLink = visit.getHighestPriorityLink();
    logger.info("after visiting link1: highPrioLink = {}", highPrioLink);
    assertThat(highPrioLink).isPresent();
    assertThat(highPrioLink.get()).isEqualTo(link2);

    visit.markVisited(link2);
    highPrioLink = visit.getHighestPriorityLink();
    logger.info("after visiting link2: highPrioLink = {}", highPrioLink);
    assertThat(highPrioLink).isPresent();
    assertThat(highPrioLink.get()).isEqualTo(link3);

    visit.markVisited(link3);
    highPrioLink = visit.getHighestPriorityLink();
    logger.info("after visiting link3: highPrioLink = {}", highPrioLink);
    assertThat(highPrioLink).isPresent();
    assertThat(highPrioLink.get()).isEqualTo(link4);

    visit.markVisited(link4);
    highPrioLink = visit.getHighestPriorityLink();
    logger.info("after visiting link4: highPrioLink = {}", highPrioLink);
    assertThat(highPrioLink).isEmpty();
  }

  private PrioritizedLink makeRandomLinkWithPriority(double priority) {
    String suffix = RandomStringUtils.randomAlphabetic(8);
    HttpUrl url = HttpUrl.get("http://localhost/" + suffix);
    return new PrioritizedLink(new Link(url, suffix), priority);
  }

}