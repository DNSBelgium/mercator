package be.dnsbelgium.mercator.mvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/")
public class SearchController {

//    private final Repository repository;
//    private final VisitRepository visitRepository;

  private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

//    @Autowired
//    public SearchController(Repository repository, VisitRepository visitRepository) {
//      this.repository = repository;
//      this.visitRepository = visitRepository;
//    }

    @GetMapping("/search")
    public String search(Model model, @RequestParam(name = "search", defaultValue = "") String search) {
//        logger.info("search for [{}]", search);
//        model.addAttribute("search", search);
//        var visits = repository.findDone(search);
//        logger.info("Search for {} => found {} visits", search, visits.size());
//        model.addAttribute("visits", visits);
        return "search-results";
    }

    @GetMapping("/visits/{id}")
    public String visit(Model model, @PathVariable(name = "id") String visitId) {
//        logger.info("/visits/{}", visitId);
//        model.addAttribute("visitId", visitId);
//
//        List<WebCrawlResult> webCrawlResults = visitRepository.findWebCrawlResults(visitId);
//        model.addAttribute("webCrawlResults", webCrawlResults);
//
//        // TODO:  retrieve records for all tables
//
        return "visit-details";
    }

}
