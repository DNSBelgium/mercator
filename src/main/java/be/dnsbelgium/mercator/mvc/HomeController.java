package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.DuckDataSource;
import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.metrics.Threads;
import be.dnsbelgium.mercator.scheduling.Scheduler;
import be.dnsbelgium.mercator.scheduling.WorkQueue;
import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.File;
import java.util.List;

@Controller
@RequestMapping("/")
public class HomeController {

    private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

  private final WorkQueue workQueue;
    private final DuckDataSource dataSource;
    private final Scheduler scheduler;

   public HomeController(WorkQueue workQueue, DuckDataSource dataSource, Scheduler scheduler) {
      this.workQueue = workQueue;
      this.dataSource = dataSource;
      this.scheduler = scheduler;
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


//    @GetMapping("/submit_crawls")
//    @ResponseBody
//    public String submitCrawls(@RequestParam(name = "numberOfCrawls", defaultValue = "100") int numberOfCrawls) {
//        logger.info("submitCrawls called: numberOfCrawls = {}", numberOfCrawls);
//        String query = "select domain_name from '%s' limit ?".formatted(trancoLocation.getAbsolutePath());
//        logger.info("query = {}", query);
//        JdbcClient jdbcClient = JdbcClient.create(dataSource);
//        List<String> names = jdbcClient
//            .sql(query)
//            .param(numberOfCrawls)
//            .query(String.class)
//            .list();
//        logger.info("names.size = {}", names.size());
//        for (String domainName : names) {
//            VisitRequest visitRequest = new VisitRequest(domainName);
//            // workQueue.add(visitRequest);
//            // todo: move code to service (if we want to keep it)
//            // We could also do it in one statement without the loop
//            jdbcClient
//                    .sql("insert into work (visit_id, domain_name) values (?,?)")
//                    .param(visitRequest.getVisitId())
//                    .param(visitRequest.getDomainName())
//                    .update();
//        }
//        scheduler.queueWork();
//        return "We added " + names.size() + " visit requests to the queue";
//    }


    @GetMapping("/submit_crawl")
    public String submitCrawlForm(Model model) {
        logger.info("submitCrawlForm: model = {}", model);
        return "submit-crawl";
    }

    @PostMapping("/submit_crawl")
    public String submitCrawl(@RequestParam String domainName,
                              RedirectAttributes redirectAttributes) {
        logger.info("/submit_crawl called with domainName = {}", domainName);
        VisitRequest visitRequest = new VisitRequest(domainName);
        workQueue.add(visitRequest);
        redirectAttributes.addFlashAttribute("visitRequest", visitRequest);
        return "redirect:submit_crawl";
    }

}

