package be.dnsbelgium.mercator.vat.wappalyzer.jappalyzer;

import be.dnsbelgium.mercator.vat.domain.Page;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Getter
public class JappalyzerPage implements Abortable {

  private final Page page;

  private final Instant analysisStarted;
  private final Duration maxDuration;
  private boolean shouldAbort;
  private final boolean measureDurationPerTechnology;

  private final Map<String,Long> millisPerTechnology = new HashMap<>();

  private static final Logger logger = LoggerFactory.getLogger(JappalyzerPage.class);

  public JappalyzerPage(Page page, Instant analysisStarted, Duration maxDuration) {
    this.page = page;
    this.analysisStarted = analysisStarted;
    this.shouldAbort = false;
    this.maxDuration = maxDuration;
    this.measureDurationPerTechnology = false;
  }

  public Duration getDuration() {
    return Duration.between(analysisStarted, Instant.now());
  }

  public boolean shouldAbort() {
    if (shouldAbort) {
      return true;
    }
    var duration =  Duration.between(analysisStarted, Instant.now());
    if (duration.compareTo(maxDuration) > 0) {
      shouldAbort = true;
      // we should log this only once per JappalyzerPage
      logger.info("Jappalyzer for {} took {} > {} => aborting", page.getUrl(), duration, maxDuration);
      if (measureDurationPerTechnology) {
        logDurationPerTechnology();
      }
      return true;
    }
    return false;
  }

  public void record(String name, long millis) {
    if (measureDurationPerTechnology) {
      millisPerTechnology.put(name, millis);
    }
  }

  public void logDurationPerTechnology() {
    // disabled by default, but can be used to check which technologies take more time
    // Might also be better to store data in a duckdb table instead of writing logs ...
    for (Map.Entry<String, Long> entry : millisPerTechnology.entrySet()) {
      logger.info("url={} tech={} millis={}", page.getUrl(), entry.getKey(), entry.getValue());
    }
  }

}
