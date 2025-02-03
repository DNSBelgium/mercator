package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.metrics.Threads;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/")
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

   public HomeController() {
    }

    @GetMapping
    public String index() {
        return "index";
    }

    @GetMapping("/hello")
    public String hello() {
        return "hello";
    }

    @GetMapping("/test-htmx")
    public String test_htmx() {
        return "test-htmx";
    }


    @GetMapping("/hello_htmx")
    @ResponseBody
    public String hello_htmx() {
      return Threads.logInfo();
    }



    @GetMapping("/submit_crawl")
    public String submitCrawlForm(Model model) {
        logger.info("submitCrawlForm: model = {}", model);
        return "submit-crawl";
    }

}

