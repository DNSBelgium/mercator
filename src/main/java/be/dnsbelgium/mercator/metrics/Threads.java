package be.dnsbelgium.mercator.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class is used to get an idea of how many threads are busy with what. *
 */
@Component
public class Threads {

  private final MeterRegistry meterRegistry;

  public static final AtomicInteger PROCESS_MESSAGE = new AtomicInteger(0);
  public static final AtomicInteger SMTP = new AtomicInteger(0);
  public static final AtomicInteger DNS = new AtomicInteger(0);
  public static final AtomicInteger WEB = new AtomicInteger(0);
  public static final AtomicInteger TLS = new AtomicInteger(0);
  public static final AtomicInteger FEATURE_EXTRACTION = new AtomicInteger(0);
  public static final AtomicInteger SAVE = new AtomicInteger(0);
  public static final AtomicInteger POST_SAVE = new AtomicInteger(0);

  private static final Logger logger = LoggerFactory.getLogger(Threads.class);

  public Threads(MeterRegistry meterRegistry) {
    this.meterRegistry = meterRegistry;
  }

  @PostConstruct
  public void init() {
    meterRegistry.gauge("threads.smtp", SMTP);
    meterRegistry.gauge("threads.dns", DNS);
    meterRegistry.gauge("threads.web", WEB);
    meterRegistry.gauge("threads.tls", TLS);
    meterRegistry.gauge("threads.feature.extraction", FEATURE_EXTRACTION);
    meterRegistry.gauge("threads.save", SAVE);
    meterRegistry.gauge("threads.post.save", POST_SAVE);
  }

  public static String logInfo() {
    int activeThreads = Thread.activeCount();
    // get a snapshot
    int smtp = SMTP.get();
    int dns = DNS.get();
    int web = WEB.get();
    int tls = TLS.get();
    int featureExtraction = FEATURE_EXTRACTION.get();
    int database = SAVE.get();
    int postSave = POST_SAVE.get();
    int tracked = smtp + dns + web + tls + featureExtraction + database + postSave;
    int processMessage = Threads.PROCESS_MESSAGE.get();
    if (tracked > activeThreads) {
      logger.error("trackedThreads={} > activeThreads={} which is weird!",  tracked, activeThreads);
    }
    double pct_smtp = smtp / (double) tracked;
    double pct_dns = dns / (double) tracked;
    double pct_web = web / (double) tracked;
    double pct_tls = tls / (double) tracked;
    double pct_featureExtraction = featureExtraction / (double) tracked;
    double pct_database = database / (double) tracked;
    double pct_postSave = postSave / (double) tracked;
    logger.info("active:{}, tracked:{}", activeThreads, tracked);
    logger.info("percentages: smtp: {}, dns: {}, web: {} tls: {}, features: {}, database: {}, postSave: {}",
            pct_smtp, pct_dns, pct_web, pct_tls, pct_featureExtraction, pct_database, pct_postSave
    );
    logger.info("Absolute numbers: smtp: {}, dns: {}, web: {} tls: {}, features: {}, database: {}, postSave: {}",
            smtp, dns, web, tls, featureExtraction, database, postSave
    );
    logger.info("process message: {}", processMessage);

    return
        "<p>Active: " + activeThreads + "</p>" +
        "<p>Tracked: " + tracked +"</p>" +
        "<p>process message: " + processMessage +"</p>" +
        "<p>Numbers    : smtp=" + smtp + " dns=" + dns + " web=" + web + " tls=" + tls + " features=" + featureExtraction + " database=" + database + " postSave=" + postSave + "</p>" +
        "<p>Percentages: smtp=" + pct_smtp + " dns=" + pct_dns + " web=" + pct_web + " tls=" + pct_tls + " features=" + pct_featureExtraction + " database=" + pct_database + " postSave=" + pct_postSave + "</p>";
  }


}
