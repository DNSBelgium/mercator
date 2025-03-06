package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.TlsRepository;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
public class TlsSearchController {

  private static final Logger logger = LoggerFactory.getLogger(TlsSearchController.class);

  private final TlsRepository tlsRepository;

  public TlsSearchController(TlsRepository tlsRepository) {
    this.tlsRepository = tlsRepository;
  }

  /**
   * show the most recent TLS crawl result for give domain name
   * @param domainName the domainName to search for
   * @return the view
   */
  @GetMapping("/search/tls/latest")
  public String getLatest(Model model, @RequestParam(name = "domainName") String domainName) {
    logger.info("domainName = {}", domainName);
    model.addAttribute("domainName", domainName);
    Optional<TlsCrawlResult> crawlResult = tlsRepository.findLatestCrawlResult(domainName);
    if (crawlResult.isPresent()) {
      // TODO: maybe we should just render ONE crawlResult in the view??
      // In other words the view should expect ONE TlsCrawlResult instead of a list
      model.addAttribute("tlsCrawlResults", List.of(crawlResult.get()));
      logger.debug("We found {} for {}", crawlResult, domainName);
    } else {
      logger.debug("No crawl result found for domainName = {}", domainName);
    }
    return "visit-details-tls";
  }

  @GetMapping("/search/tls/ids")
  public String getTlsIds(Model model, @RequestParam(name = "domainName") String domainName) {
    logger.info("domainName = {}", domainName);
    model.addAttribute("domainName", domainName);
    List<String> idList = tlsRepository.searchVisitIds(domainName);
    if (!idList.isEmpty()) {
      model.addAttribute("idList", idList);
      logger.debug("We found {} for {}", idList, domainName);
    }
    return "search-results-tls";
  }

  @GetMapping("/search/tls/ids")
  public String getTls(Model model, @RequestParam(name = "id") String id) {
    logger.info("id = {}", id);
    model.addAttribute("id", id);
    Optional<TlsCrawlResult> tlsCrawlResult = tlsRepository.findByVisitId(id);
    if(tlsCrawlResult.isPresent()) {
      model.addAttribute("tlsCrawlResults", List.of(tlsCrawlResult.get()));
    }
    return "search-results-tls";
  }


}
