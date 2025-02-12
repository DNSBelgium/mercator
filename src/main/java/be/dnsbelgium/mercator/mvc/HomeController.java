package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.scheduling.WorkQueue;
import be.dnsbelgium.mercator.wappalyzer.TechnologyAnalyzer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.http.HttpClient;
import java.util.Set;

@Controller
@RequestMapping("/")
public class HomeController { 

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

    private final WorkQueue workQueue;
    private final TechnologyAnalyzer technologyAnalyzer;

    // Constructor
    public HomeController(WorkQueue workQueue) {
        this.workQueue = workQueue;
        // Initialize TechnologyAnalyzer with HttpClient
        this.technologyAnalyzer = new TechnologyAnalyzer(HttpClient.newHttpClient());
    }

    @GetMapping("/submit_crawl")
    public String submitCrawlForm(Model model) {
        logger.debug("submitCrawlForm: model = {}", model);
        return "submit-crawl";
    }

    @PostMapping("/submit_crawl")
    public String submitCrawl(@RequestParam String domainName,
                              RedirectAttributes redirectAttributes) {
        logger.info("/submit_crawl called with domainName = {}", domainName);

        VisitRequest visitRequest = new VisitRequest(domainName);
        workQueue.add(visitRequest);

        String url = "https://" + domainName; 
        Set<String> detectedTechnologies = technologyAnalyzer.analyze(url);

        logger.info("Detected technologies for domain '{}': {}", domainName, detectedTechnologies);

        redirectAttributes.addFlashAttribute("visitRequest", visitRequest);
        redirectAttributes.addFlashAttribute("detectedTechnologies", detectedTechnologies);

        return "redirect:submit_crawl";
    }
}
