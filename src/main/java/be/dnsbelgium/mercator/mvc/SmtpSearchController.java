package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.SearchVisitIdResultItem;
import be.dnsbelgium.mercator.persistence.SmtpRepository;

import be.dnsbelgium.mercator.smtp.dto.SmtpVisit;
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
  private final SmtpRepository smtpRepository;

  public SmtpSearchController(SmtpRepository smtpRepository) {
    this.smtpRepository = smtpRepository;
  }

  @GetMapping("/search/smtp/latest")
  public String findLatestResult(Model model, @RequestParam("domainName") String domainName) {
    model.addAttribute("domainName", domainName);
    Optional<SmtpVisit> smtpVisitResult = smtpRepository.findLatestResult(domainName);
    if (smtpVisitResult.isPresent()) {
      model.addAttribute("smtpVisitResult", smtpVisitResult.get());
    }
    return "visit-details-smtp";
  }

  @GetMapping("/search/smtp/ids")
  public String searchVisitIds(Model model, @RequestParam(name = "domainName") String domainName) {
    logger.info("search for [{}]", domainName);
    List<SearchVisitIdResultItem> visitIds = smtpRepository.searchVisitIds(domainName);
    logger.debug("getSmtpIds for {} => {}", domainName, visitIds);
    model.addAttribute("domainName", domainName);
    model.addAttribute("visitIds", visitIds);
    return "search-results-smtp";
  }

  @GetMapping("/search/smtp/id")
  public String findByVisitId(Model model, @RequestParam(name = "visitId") String visitId) {
    logger.info("/visits/smtp/{}", visitId);
    model.addAttribute("visitId", visitId);
    Optional<SmtpVisit> smtpVisitResult = smtpRepository.findByVisitId(visitId);
    if (smtpVisitResult.isPresent()) {
      model.addAttribute("smtpVisitResult", smtpVisitResult.get());
    }
    return "visit-details-smtp";
  }
}
