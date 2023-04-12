package be.dnsbelgium.mercator.api.status;

import be.dnsbelgium.mercator.content.persistence.ContentCrawlResult;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResultRepository;
import be.dnsbelgium.mercator.content.persistence.WappalyzerResult;
import be.dnsbelgium.mercator.content.persistence.WappalyzerResultRepository;
import be.dnsbelgium.mercator.dns.persistence.Request;
import be.dnsbelgium.mercator.dns.persistence.RequestRepository;
import be.dnsbelgium.mercator.smtp.persistence.entities.CrawlStatus;
import be.dnsbelgium.mercator.smtp.persistence.entities.SmtpCrawlResult;
import be.dnsbelgium.mercator.smtp.persistence.repositories.SmtpCrawlResultRepository;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Component
public class CrawlComponentStatusService {

  private final RequestRepository requestRepository;
  private final SmtpCrawlResultRepository smtpRepository;
  private final ContentCrawlResultRepository mupetsepository;
  private final WappalyzerResultRepository wappalyzerRepository;

  public CrawlComponentStatusService(RequestRepository requestRepository, SmtpCrawlResultRepository smtpRepository, ContentCrawlResultRepository mupetsepository, WappalyzerResultRepository wappalyzerRepository) {
    this.requestRepository = requestRepository;
    this.smtpRepository = smtpRepository;
    this.mupetsepository = mupetsepository;
    this.wappalyzerRepository = wappalyzerRepository;
  }

  public CrawlComponentStatus getCrawlComponentStatus(UUID visitId) throws ExecutionException, InterruptedException {

    var dnsFuture = CompletableFuture.supplyAsync(() -> requestRepository.findByVisitId(visitId));
    var smtpFuture = CompletableFuture.supplyAsync(() -> smtpRepository.findFirstByVisitId(visitId));
    var muppetsFuture = CompletableFuture.supplyAsync(() -> mupetsepository.findByVisitId(visitId));
    var wappalyzerFuture = CompletableFuture.supplyAsync(() -> wappalyzerRepository.findByVisitId(visitId));

    CompletableFuture.allOf(dnsFuture, smtpFuture, muppetsFuture, wappalyzerFuture).get();

    List<Request> request = dnsFuture.exceptionally((ex -> Collections.emptyList())).get();
    Optional<SmtpCrawlResult> smtpResult = smtpFuture.exceptionally((ex -> Optional.empty())).get();
    List<ContentCrawlResult> muppetsResults = muppetsFuture.exceptionally((ex -> Collections.emptyList())).get();
    Optional<WappalyzerResult> wappalyzerResult = wappalyzerFuture.exceptionally((ex -> Optional.empty())).get();

    return new CrawlComponentStatus(visitId,
      request.stream().anyMatch(Request::isOk),
      smtpResult.map(result -> result.getCrawlStatus() == CrawlStatus.OK).orElse(false),
      muppetsResults.stream().anyMatch(ContentCrawlResult::isOk),
      wappalyzerResult.map(WappalyzerResult::isOk).orElse(false));
  }
}
