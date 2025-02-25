package be.dnsbelgium.mercator.visits;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.dns.CrawlStatus;
import be.dnsbelgium.mercator.dns.domain.DnsCrawlResult;
import be.dnsbelgium.mercator.dns.domain.DnsCrawlService;
import be.dnsbelgium.mercator.smtp.SmtpCrawler;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpVisit;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import be.dnsbelgium.mercator.tls.ports.TlsCrawler;
import be.dnsbelgium.mercator.vat.WebCrawler;
import be.dnsbelgium.mercator.vat.crawler.persistence.WebCrawlResult;
import be.dnsbelgium.mercator.wappalyzer.TechnologAnalyzerWebCrawler;
import be.dnsbelgium.mercator.wappalyzer.crawler.persistence.TechnologyAnalyzerWebCrawlResult;
import be.dnsbelgium.mercator.metrics.Threads;
import be.dnsbelgium.mercator.persistence.Repository;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("SqlDialectInspection")
@Service
public class MainCrawler {

  private final DnsCrawlService dnsCrawlService;
  private final WebCrawler webCrawler;
  private final TlsCrawler tlsCrawler;
  private final SmtpCrawler smtpCrawler;
  private final MeterRegistry meterRegistry;
  private final TechnologAnalyzerWebCrawler technologAnalyzerWebCrawler;

  private final Repository repository;
  private final VisitService visitService;

  @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
  private final List<CrawlerModule<?>> crawlerModules;

  @Value("${smtp.enabled:true}")
  private boolean smtpEnabled;

  private static final Logger logger = LoggerFactory.getLogger(MainCrawler.class);

  @Autowired
  public MainCrawler(DnsCrawlService dnsCrawlService,
      WebCrawler webCrawler,
      Repository repository,
      SmtpCrawler smtpCrawler,
      TlsCrawler tlsCrawler,
      MeterRegistry meterRegistry,
      VisitService visitService,
      TechnologAnalyzerWebCrawler technologAnalyzerWebCrawler) {
    this.dnsCrawlService = dnsCrawlService;
    this.webCrawler = webCrawler;
    this.tlsCrawler = tlsCrawler;
    this.repository = repository;
    this.meterRegistry = meterRegistry;
    this.visitService = visitService;
    this.smtpCrawler = smtpCrawler;
    this.technologAnalyzerWebCrawler = technologAnalyzerWebCrawler;
    crawlerModules = new ArrayList<>();
  }

  @SuppressWarnings("unused")
  public void register(CrawlerModule<?> crawlerModule) {
    crawlerModules.add(crawlerModule);
  }

  public void visit(VisitRequest visitRequest) {
    VisitResult visitResult = collectData(visitRequest);
    visitService.save(visitResult);
    repository.markDone(visitRequest);
    postSave(visitResult);
  }

  private void postSave(VisitResult visitResult) {
    Threads.POST_SAVE.incrementAndGet();
    try {

      var dataPerModule = visitResult.getCollectedData();

      if (dataPerModule != null) {
        for (CrawlerModule<?> crawlerModule : dataPerModule.keySet()) {
          var data = dataPerModule.get(crawlerModule);
          crawlerModule.afterSave(data);
        }
      }

    } finally {
      Threads.POST_SAVE.decrementAndGet();
    }
  }

  private VisitResult collectData(VisitRequest visitRequest) {
    Timer.Sample sample = Timer.start(meterRegistry);
    try {
      DnsCrawlResult dnsCrawlResult = dnsCrawlService.visit(visitRequest);
      if (dnsCrawlResult.getStatus() == CrawlStatus.NXDOMAIN) {
        return VisitResult.builder()
            .visitRequest(visitRequest)
            .dnsCrawlResult(dnsCrawlResult)
            .build();
      }
      Map<CrawlerModule<?>, List<?>> collectedData = new HashMap<>();

      List<WebCrawlResult> webCrawlResults = webCrawler.collectData(visitRequest);
      collectedData.put(webCrawler, webCrawlResults);

      List<TlsCrawlResult> tlsCrawlResults = tlsCrawler.collectData(visitRequest);
      collectedData.put(tlsCrawler, tlsCrawlResults);

      logger.debug("crawling Wappalyzer for {}", visitRequest.getDomainName());
      List<TechnologyAnalyzerWebCrawlResult> wappalyzerResults = technologAnalyzerWebCrawler.collectData(visitRequest);
      logger.debug("DONE crawling Wappalyzer for {} => {}", visitRequest.getDomainName(), wappalyzerResults);
      collectedData.put(technologAnalyzerWebCrawler, wappalyzerResults);

      if (smtpEnabled) {
        logger.info("crawling SMTP for {}", visitRequest.getDomainName());
        List<SmtpVisit> smtpVisits = smtpCrawler.collectData(visitRequest);
        logger.info("DONE crawling SMTP for {} => {}", visitRequest.getDomainName(), smtpVisits);
        collectedData.put(smtpCrawler, smtpVisits);
      }

      return VisitResult.builder()
          .visitRequest(visitRequest)
          .dnsCrawlResult(dnsCrawlResult)
          .collectedData(collectedData)
          .build();
    } finally {
      sample.stop(meterRegistry.timer("crawler.collectData"));
    }
  }

}