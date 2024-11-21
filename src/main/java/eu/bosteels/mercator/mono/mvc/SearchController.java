package eu.bosteels.mercator.mono.mvc;

import eu.bosteels.mercator.mono.persistence.Repository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/")
public class SearchController {

    private final Repository repository;
    private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

    @Autowired
    public SearchController(Repository repository) {
        this.repository = repository;
    }

    @GetMapping("/search")
    public String search(Model model, @RequestParam(name = "search", defaultValue = "") String search) {
        logger.info("search for [{}]", search);
        model.addAttribute("search", search);
        var visits = repository.findDone(search);
        logger.info("Search for {} => found {} visits", search, visits.size());
        model.addAttribute("visits", visits);
        return "search-results";
    }

    @GetMapping("/visits/{id}")
    public String visit(Model model, @PathVariable(name = "id") String visitId) {
        logger.info("/visits/{}", visitId);
        model.addAttribute("visitId", visitId);
        //var visits = repository.findDone(search);

        // TODO:  create records for all tables
        // retrieve lists of these records and put them in the model
        // or use the same classes we used for inserting ?

        return "visit-details";
    }

}
