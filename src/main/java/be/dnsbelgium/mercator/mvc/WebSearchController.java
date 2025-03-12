package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.WebRepository;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

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
        Optional<WebCrawlResult> webCrawlResult = webRepository.findLatestResult(domainName);
        if (webCrawlResult.isPresent()) {
            model.addAttribute("webCrawlResult", webCrawlResult.get());
        }
        return "visit-details-web";
    }

    @GetMapping("/search/web/ids")
    public String searchVisitIds(Model model, @RequestParam(name = "domainName") String domainName) {
        logger.info("search for [{}]", domainName);
        List<String> visitIds = webRepository.searchVisitIds(domainName);
        logger.info("visitIds found: {}", visitIds);
        model.addAttribute("visitIds", visitIds);
        return "search-results-web";
    }

    @GetMapping("/search/web/id")
    public String findByVisitId(Model model, @RequestParam(name = "visitId") String visitId) {
        logger.info("/visits/web/{}", visitId);
        Optional<WebCrawlResult> webCrawlResult = webRepository.findByVisitId(visitId);
        logger.info(webCrawlResult.toString());
        if (webCrawlResult.isPresent()) {
            model.addAttribute("webCrawlResult", webCrawlResult.get());
        }
        return "visit-details-web";
    }
}
