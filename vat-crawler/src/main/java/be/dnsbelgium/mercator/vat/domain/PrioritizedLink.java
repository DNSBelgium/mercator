package be.dnsbelgium.mercator.vat.domain;

import lombok.Getter;
import org.slf4j.Logger;

import java.util.StringJoiner;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * A Link that has a priority and thus a natural order
 */
@Getter
public class PrioritizedLink extends Link implements Comparable<PrioritizedLink> {

  private static final Logger logger = getLogger(PrioritizedLink.class);

  /**
   * A value between 0.0 and 1.0 where higher means more priority
   */
  private final double priority;

  public PrioritizedLink(Link link, double priority) {
    super(link);
    this.priority = priority;
  }

  @Override
  public int compareTo(PrioritizedLink o) {
    // -1 since natural order = highest priority first
    return -1 * Double.compare(this.priority, o.getPriority());
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", PrioritizedLink.class.getSimpleName() + "[", "]")
        .add("url=" + getUrl())
        .add("text=" + getText())
        .add("priority=" + priority)
        .add("referer=" + getReferer())
        .toString();
  }

}
