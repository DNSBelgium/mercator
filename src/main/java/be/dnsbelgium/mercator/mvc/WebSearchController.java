package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.SearchVisitIdResultItem;
import be.dnsbelgium.mercator.persistence.WebRepository;
import be.dnsbelgium.mercator.web.domain.WebCrawlResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalIsPresent")
@Controller
public class WebSearchController {
    private static final Logger logger = LoggerFactory.getLogger(WebSearchController.class);
    private final WebRepository webRepository;

    public WebSearchController(WebRepository webRepository) {
        this.webRepository = webRepository;
    }

    @GetMapping("/search/web/latest")
    public String findLatestResult(Model model, @RequestParam("domainName") String domainName) {
        model.addAttribute("domainName", domainName);
        Optional<WebCrawlResult> webCrawlResult = webRepository.findLatestResult(domainName);
        if (webCrawlResult.isPresent()) {
            // we add year and month to speed up querying for related data (like getResponseBody)
            model.addAttribute("webCrawlResult", webCrawlResult.get());
            model.addAttribute("year", webCrawlResult.get().year());
            model.addAttribute("month", webCrawlResult.get().month());
        }
        return "visit-details-web";
    }

    @GetMapping("/search/web/response-body")
    @ResponseBody
    public String getResponseBody(
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month,
            @RequestParam(name = "visitId") String visitId,
            @RequestParam(name = "finalUrl") String finalUrl) {
      logger.info("getResponseBody: year = {}, month = {}", year, month);
      Optional<String> responseBody = webRepository.getResponseBody(year, month, visitId, finalUrl);
      return responseBody.orElse("");
    }

    @GetMapping("/search/web/response-body-raw")
    public String getResponseBodyRaw(
            Model model,
            @RequestParam(name = "year") int year,
            @RequestParam(name = "month") int month,
            @RequestParam(name = "visitId") String visitId,
            @RequestParam(name = "finalUrl") String finalUrl) {
      logger.info("getResponseBodyRaw: year = {}, month = {}", year, month);
      model.addAttribute("visitId", visitId);
      model.addAttribute("finalUrl", finalUrl);
      Optional<String> responseBody = webRepository.getResponseBody(year, month, visitId, finalUrl);
      if (responseBody.isPresent()) {
        // add line breaks before every element to (hopefully) make it more readable.
        String formattedResponseBody = responseBody.get().replace("<", "\n<");
        model.addAttribute("responseBody", formattedResponseBody);
      }
      return "raw-response-body";
    }

    @GetMapping("/search/web/ids")
    public String searchVisitIds(Model model, @RequestParam(name = "domainName") String domainName) {
        logger.info("search for [{}]", domainName);
        List<SearchVisitIdResultItem> visitIds = webRepository.searchVisitIds(domainName);
        logger.info("domainName={} => found: {}", domainName, visitIds);
        model.addAttribute("domainName", domainName);
        model.addAttribute("visitIds", visitIds);
        return "search-results-web";
    }

    @GetMapping("/search/web/id")
    public String findByVisitId(Model model, @RequestParam(name = "visitId") String visitId) {
        model.addAttribute("visitId", visitId);
        logger.info("/visits/web/{}", visitId);
        Optional<WebCrawlResult> webCrawlResult = webRepository.findByVisitId(visitId);
        logger.info(webCrawlResult.toString());
        if (webCrawlResult.isPresent()) {
            model.addAttribute("webCrawlResult", webCrawlResult.get());
        }
        return "visit-details-web";
    }
}
