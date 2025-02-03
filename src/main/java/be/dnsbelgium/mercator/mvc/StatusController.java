package be.dnsbelgium.mercator.mvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StatusController {

    private static final Logger logger = LoggerFactory.getLogger(StatusController.class);

//    private final Repository repository;
//    private final VisitRepository visitRepository;

//    public StatusController(Repository repository, VisitRepository visitRepository) {
//        this.repository = repository;
//        this.visitRepository = visitRepository;
//    }

    @GetMapping("/status")
    public String status(Model model) {
        logger.info("/status called");
//        var perHour = repository.getRecentCrawlRates(Repository.Frequency.PerHour, 120);
//        var perMinute = repository.getRecentCrawlRates(Repository.Frequency.PerMinute, 120);
//        Stats stats = repository.getStats();
//        var databaseSize = visitRepository.databaseSize();
//        model.addAttribute("perHour", perHour);
//        model.addAttribute("perMinute", perMinute);
//        model.addAttribute("stats", stats);
//        model.addAttribute("databaseSize", databaseSize);
        return "status";
    }

}
