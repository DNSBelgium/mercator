package be.dnsbelgium.mercator.vat.wappalyzer.jappalyzer;

import be.dnsbelgium.mercator.vat.domain.Page;
import lombok.Getter;

import java.time.Duration;
import java.time.Instant;

@Getter
public class JappalyzerPage {

  private Page page;

  private Instant analysisStarted;

  public JappalyzerPage(Page page, Instant analysisStarted) {
    this.page = page;
    this.analysisStarted = analysisStarted;
  }

  public Duration getDuration() {
    return Duration.between(analysisStarted, Instant.now());
  }

}
