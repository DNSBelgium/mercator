package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.SearchRepository;
import be.dnsbelgium.mercator.smtp.dto.SmtpConversation;
import be.dnsbelgium.mercator.tls.domain.TlsCrawlResult;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
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
public class SearchController {

  private static final Logger logger = LoggerFactory.getLogger(SearchController.class);
  private final SearchRepository searchRepository;

  public SearchController(SearchRepository searchRepository) {
    this.searchRepository = searchRepository;
  }


  @GetMapping("/search/web")
  public String searchWeb(Model model, 
  @RequestParam(name = "domainName", defaultValue = "") String domainName,
  @RequestParam(name = "fetch_latest", defaultValue = "") String fetch_latest) throws JsonProcessingException {
    if (domainName.isEmpty()) {
      return "search";
    }
    if (fetch_latest.contains("on")) {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      Optional<String> json = searchRepository.searchlatestWebResult();
      if (json.isPresent()) {
        WebCrawlResult webCrawlResult = objectMapper.readValue(json.get(), WebCrawlResult.class);;
        model.addAttribute("webCrawlResults", List.of(webCrawlResult));
        logger.info("webcrawlresult = {}", webCrawlResult);
      }
      return "visit-details-web";

    }

    logger.info("search for [{}]", domainName);

    List<WebCrawlResult> webCrawlResults = searchRepository.searchVisitIdsWeb(domainName);
    logger.info("our results: " + webCrawlResults);

    model.addAttribute("search", domainName);
    model.addAttribute("visits", webCrawlResults);

    return "search-results-web";
  }


  @GetMapping("/search/tls")
  public String searchTls(Model model, 
  @RequestParam(name = "domainName", defaultValue = "") String domainName,
  @RequestParam(name = "fetch_latest", defaultValue = "") String fetch_latest) throws JsonProcessingException {
    if (domainName.isEmpty()) {
      return "search";
    }
    if (fetch_latest.contains("on")) {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      Optional<String> json = searchRepository.searchlatestTlsResult();
      if (json.isPresent()) {
        TlsCrawlResult tlsCrawlResult = objectMapper.readValue(json.get(), TlsCrawlResult.class);;
        model.addAttribute("tlsCrawlResults", List.of(tlsCrawlResult));
        logger.info("tlsCrawlResult = {}", tlsCrawlResult);
      }
      return "visit-details-tls";

    }

    logger.info("search for [{}]", domainName);

    List<TlsCrawlResult> tlsCrawlResults = searchRepository.searchVisitIdsTls(domainName);
    logger.info("our results: " + tlsCrawlResults);

    model.addAttribute("category", "tls");
    model.addAttribute("visits", tlsCrawlResults);

    return "search-results-tls";
  }

  @GetMapping("/search/smtp")
  public String searchSmtp(Model model, 
  @RequestParam(name = "domainName", defaultValue = "") String domainName,
  @RequestParam(name = "fetch_latest", defaultValue = "") String fetch_latest) throws JsonProcessingException {
    if (domainName.isEmpty()) {
      return "search";
    }
    if (fetch_latest.contains("on")) {
      ObjectMapper objectMapper = new ObjectMapper();
      objectMapper.registerModule(new JavaTimeModule());
      Optional<String> json = searchRepository.searchlatestSmtpResult();
      if (json.isPresent()) {
        SmtpConversation smtpConversation = objectMapper.readValue(json.get(), SmtpConversation.class);;
        model.addAttribute("smtpConversationResults", List.of(smtpConversation));
        logger.info("tlsCrawlResult = {}", smtpConversation);
      }
      return "visit-details-smtp";
    }
    logger.info("search for [{}]", domainName);

    List<SmtpConversation> smtpConversations = searchRepository.searchVisitIdsSmtp(domainName);
    logger.info("our results: " + smtpConversations);

    model.addAttribute("search", domainName);
    model.addAttribute("visits", smtpConversations);

    return "search-results-smtp";
  }

  @GetMapping("/visits/web")
  public String visitWeb(Model model, @RequestParam(name = "visitId", defaultValue = "") String visitId) throws JsonMappingException, JsonProcessingException {
    logger.info("/visits/web/{}", visitId);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    Optional<String> json = searchRepository.searchVisitIdWeb(visitId);
    if (json.isPresent()) {
      WebCrawlResult webCrawlResult = objectMapper.readValue(json.get(), WebCrawlResult.class);;
      model.addAttribute("webCrawlResults", List.of(webCrawlResult));
      logger.info("webcrawlresult = {}", webCrawlResult);
    }
    return "visit-details-web";
  }
  @GetMapping("/visits/smtp")
  public String visitSmtp(Model model, @RequestParam(name = "visitId", defaultValue = "") String visitId) throws JsonMappingException, JsonProcessingException {
    logger.info("/visits/smtp/{}", visitId);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    Optional<String> json = searchRepository.searchVisitIdSmtp(visitId);
    if (json.isPresent()) {
      SmtpConversation smtpConversation = objectMapper.readValue(json.get(), SmtpConversation.class);;
      model.addAttribute("smtpConversationResults", List.of(smtpConversation));
      logger.info("tlsCrawlResult = {}", smtpConversation);
    }
    return "visit-details-smtp";
  }
  @GetMapping("/visits/tls")
  public String visitTls(Model model, @RequestParam(name = "visitId", defaultValue = "") String visitId) throws JsonMappingException, JsonProcessingException {
    logger.info("/visits/tls/{}", visitId);
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    Optional<String> json = searchRepository.searchVisitIdTls(visitId);
    if (json.isPresent()) {
      TlsCrawlResult tlsCrawlResult = objectMapper.readValue(json.get(), TlsCrawlResult.class);;
      model.addAttribute("tlsCrawlResults", List.of(tlsCrawlResult));
      logger.info("tlsCrawlResult = {}", tlsCrawlResult);
    }
    return "visit-details-tls";
  }

}
