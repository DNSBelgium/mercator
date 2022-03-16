package be.dnsbelgium.mercator.api.status;

import be.dnsbelgium.mercator.content.persistence.ContentCrawlResult;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResultRepository;
import be.dnsbelgium.mercator.content.persistence.WappalyzerResult;
import be.dnsbelgium.mercator.content.persistence.WappalyzerResultRepository;
import be.dnsbelgium.mercator.dns.persistence.DnsCrawlResult;
import be.dnsbelgium.mercator.dns.persistence.DnsCrawlResultRepository;
import be.dnsbelgium.mercator.smtp.persistence.CrawlStatus;
import be.dnsbelgium.mercator.smtp.persistence.SmtpCrawlResult;
import be.dnsbelgium.mercator.smtp.persistence.SmtpCrawlResultRepository;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class CrawlComponentStatusService {

  private final DnsCrawlResultRepository dnsRepository;
  private final SmtpCrawlResultRepository smtpRepository;
  private final ContentCrawlResultRepository mupetsepository;
  private final WappalyzerResultRepository wappalyzerRepository;

  public CrawlComponentStatusService(DnsCrawlResultRepository dnsRepository, SmtpCrawlResultRepository smtpRepository, ContentCrawlResultRepository mupetsepository, WappalyzerResultRepository wappalyzerRepository) {
    this.dnsRepository = dnsRepository;
    this.smtpRepository = smtpRepository;
    this.mupetsepository = mupetsepository;
    this.wappalyzerRepository = wappalyzerRepository;
  }

  public CrawlComponentStatus getCrawlComponentStatus(UUID visitId) throws ExecutionException, InterruptedException {

    var dnsFuture = CompletableFuture.supplyAsync(() -> dnsRepository.findByVisitId(visitId));
    var smtpFuture = CompletableFuture.supplyAsync(() -> smtpRepository.findByVisitId(visitId));
    var muppetsFuture = CompletableFuture.supplyAsync(() -> mupetsepository.findByVisitId(visitId));
    var wappalyzerFuture = CompletableFuture.supplyAsync(() -> wappalyzerRepository.findByVisitId(visitId));

    CompletableFuture.allOf(dnsFuture, smtpFuture, muppetsFuture, wappalyzerFuture).get();

    Optional<DnsCrawlResult> dnsResult = dnsFuture.exceptionally((ex -> Optional.empty())).get();
    Optional<SmtpCrawlResult> smtpResult = smtpFuture.exceptionally((ex -> Optional.empty())).get();
    List<ContentCrawlResult> muppetsResults = muppetsFuture.exceptionally((ex -> Collections.emptyList())).get();
    Optional<WappalyzerResult> wappalyzerResult = wappalyzerFuture.exceptionally((ex -> Optional.empty())).get();

    return new CrawlComponentStatus(visitId,
                                    dnsResult.map(DnsCrawlResult::isOk).orElse(false),
                                    smtpResult.map(result -> result.getCrawlStatus() == CrawlStatus.OK).orElse(false),
                                    muppetsResults.stream().anyMatch(ContentCrawlResult::isOk),
                                    wappalyzerResult.map(WappalyzerResult::isOk).orElse(false));
  }
}
