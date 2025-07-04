package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.dns.dto.*;
import be.dnsbelgium.mercator.persistence.DnsRepository;
import be.dnsbelgium.mercator.persistence.SearchVisitIdResultItem;
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
public class DnsSearchController {
    private static final Logger logger = LoggerFactory.getLogger(DnsSearchController.class);
    private final DnsRepository dnsRepository;

    public DnsSearchController(DnsRepository dnsRepository) {
        this.dnsRepository = dnsRepository;
    }

    @GetMapping("/search/dns/latest")
    public String findLatestResult(Model model, @RequestParam("domainName") String domainName) {
        logger.info("Finding latest DNS result for {}", domainName);
        model.addAttribute("domainName", domainName);
        Optional<DnsCrawlResult> dnsCrawlResult = dnsRepository.findLatestResult(domainName);
        logger.info("domainName={} => dnsCrawlResult.isPresent = {}", domainName, dnsCrawlResult.isPresent());
        if (dnsCrawlResult.isPresent()) {
            model.addAttribute("dnsCrawlResult", dnsCrawlResult.get());
        }
        return "visit-details-dns";
    }

    @GetMapping("/search/dns/ids")
    public String searchVisitIds(Model model, @RequestParam(name = "domainName") String domainName) {
        logger.info("searchVisitIds for [{}]", domainName);
        List<SearchVisitIdResultItem> visitIds = dnsRepository.searchVisitIds(domainName);
        logger.info("domainName={} => found: {}", domainName, visitIds);
        model.addAttribute("domainName", domainName);
        model.addAttribute("visitIds", visitIds);
        return "search-results-dns";
    }

    @GetMapping("/search/dns/id")
    public String findByVisitId(Model model, @RequestParam(name = "visitId") String visitId) {
        model.addAttribute("visitId", visitId);
        logger.info("/visits/dns/{}", visitId);
        Optional<DnsCrawlResult> dnsCrawlResult = dnsRepository.findByVisitId(visitId);
        if (dnsCrawlResult.isPresent()) {
            model.addAttribute("dnsCrawlResult", dnsCrawlResult.get());
        }
        return "visit-details-dns";
    }
}
