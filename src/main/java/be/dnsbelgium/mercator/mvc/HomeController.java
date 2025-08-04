package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.SimpleJobRunner;
import be.dnsbelgium.mercator.metrics.Threads;
import be.dnsbelgium.mercator.schedule.JobScheduler;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

@Controller
@RequestMapping("/")
public class HomeController {

   private static final Logger logger = LoggerFactory.getLogger(HomeController.class);

   private final SimpleJobRunner simpleJobRunner;
   private final JobScheduler jobScheduler;
   private final ObjectWriter objectWriter;

  public HomeController(SimpleJobRunner simpleJobRunner, JobScheduler jobScheduler) {
     this.simpleJobRunner = simpleJobRunner;
     this.jobScheduler = jobScheduler;
     ObjectMapper objectMapper = new ObjectMapper();
     objectWriter = objectMapper.writer().withDefaultPrettyPrinter();
   }

    @GetMapping
    public String index() {
        return "index";
    }

    @GetMapping("/start_job")
    public String start_job() {
      logger.info("Starting job ...");
      try {
        simpleJobRunner.run();
        logger.info("job launched");
      } catch (Exception e) {
        logger.error("Error launching job", e);
      }
      return "redirect:/";
    }

    @GetMapping("/error")
    public String error() {
        return "error";
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

  // curl -F file=@"input.csv" http://localhost:8080/upload

  @PostMapping("/upload")
  public String handleFileUpload(@RequestParam("file") MultipartFile file,
                                 RedirectAttributes redirectAttributes) {
    logger.info("file = {}", file);
    logger.info("file.getOriginalFilename = {}", file.getOriginalFilename());
    logger.info("file.getSize = {}", file.getSize());
    int rowsAdded = jobScheduler.addToQueue(file);
    redirectAttributes.addFlashAttribute("message",
            "You successfully uploaded " + file.getOriginalFilename() + "!");
    redirectAttributes.addFlashAttribute("rowsAdded", rowsAdded);
    return "redirect:/";
  }

  @GetMapping("/stats")
  @ResponseBody
  public String stats() throws JsonProcessingException {
    // This method is very convenient to test the state after uploading a file (eg. with curl)
    // TODO: should we create a REST controller?
    Map<String, Object> stats = jobScheduler.stats();
    return objectWriter.writeValueAsString(stats);
  }
}

