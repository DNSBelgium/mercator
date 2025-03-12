package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.SmtpRepository;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;

import org.springframework.web.bind.annotation.GetMapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalIsPresent")
@Controller
public class SmtpSearchController {

  private static final Logger logger = LoggerFactory.getLogger(SmtpSearchController.class);
  private final SmtpRepository searchRepository;

  public SmtpSearchController(SmtpRepository searchRepository) {
    this.searchRepository = searchRepository;
  }

  @GetMapping("/search/smtp/latest")
  public String findLatestResult(Model model, @RequestParam("domainName") String domainName) {
    Optional<SmtpConversation> smtpConversationResult = searchRepository.findLatestResult(domainName);
    if (smtpConversationResult.isPresent()) {
      model.addAttribute("smtpConversationResult", smtpConversationResult.get());
    }
    return "visit-details-smtp";
  }

  @GetMapping("/search/smtp/ids")
  public String searchVisitIds(Model model, @RequestParam(name = "domainName") String domainName) {
    logger.info("search for [{}]", domainName);
    List<String> visitIds = searchRepository.searchVisitIds(domainName);
    logger.debug("getSmtpIds for {} => {}", domainName, visitIds);
    model.addAttribute("visitIds", visitIds);
    return "search-results-smtp";
  }

  @GetMapping("/search/smtp/id")
  public String findByVisitId(Model model, @RequestParam(name = "visitId") String visitId) {
    logger.info("/visits/smtp/{}", visitId);
    Optional<SmtpConversation> smtpConversation = searchRepository.findByVisitId(visitId);
    if (smtpConversation.isPresent()) {
      model.addAttribute("smtpConversationResult", smtpConversation.get());
    }
    return "visit-details-smtp";
  }
}
