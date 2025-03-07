package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.SmtpRepository;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.web.bind.annotation.GetMapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
public class SmtpSearchController {

  private static final Logger logger = LoggerFactory.getLogger(SmtpSearchController.class);
  private final SmtpRepository searchRepository;

  public SmtpSearchController(SmtpRepository searchRepository) {
    this.searchRepository = searchRepository;
  }




  // TODO: try to use a Boolean for fetch_latest instead of a String representing a boolean
  // TODO: or maybe better: consider using separate methods for findLatest and findAll
  // TODO: are the defaultValue's needed ?
  // TODO: searchRepository.searchlatestTlsResult should return an Optional<TlsCrawlResult> instead of a (JSON) String
  // TODO: inject ObjectMapper from application context instead of creating a new one very time (and should move to Repository)
  // TODO: same things for searchSmtp and searchweb
  // TODO: make sure the test methods do not depend on state that is not in git (like tls.parquet)
  // * use a mocked Repository when testing the SearchController
  // * create (and remove) the needed parquet files IN the test for the repository (or in a BeforeEach method)


  @GetMapping("/search/smtp/latest")
  public String getLatestSmtp(Model model, @RequestParam("domainName") String domainName) {
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    Optional<SmtpConversation> smtpConversationResult = searchRepository.findLatestResult(domainName);
    model.addAttribute("smtpConversationResults", List.of(smtpConversationResult));
    return "visit-details-smtp";
  }

  @GetMapping("/search/smtp/ids")
  public String getSmtpIds(Model model, @RequestParam(name = "domainName") String domainName) {
    logger.info("search for [{}]", domainName);
    List<String> idLIst = searchRepository.searchVisitIds(domainName);
    logger.info("our results: " + idLIst);
    model.addAttribute("visitIds", idLIst);
    return "search-results-smtp";
  }

  @GetMapping("/search/smtp/id")
  public String getSmtp(Model model, @RequestParam(name = "visitId") String visitId) {
    logger.info("/visits/smtp/{}", visitId);
    Optional<SmtpConversation> smtpConversation = searchRepository.findByVisitId(visitId);
      model.addAttribute("smtpConversationResults", List.of(smtpConversation));
      logger.info("tlsCrawlResult = {}", smtpConversation);
    return "visit-details-smtp";
  }
}
