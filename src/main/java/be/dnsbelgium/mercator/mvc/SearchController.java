package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.DuckDataSource;
import be.dnsbelgium.mercator.vat.domain.WebCrawlResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.web.bind.annotation.GetMapping;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Optional;

@Controller
public class SearchController {

  private static final Logger logger = LoggerFactory.getLogger(SearchController.class);

  @GetMapping("/search")
  public String search(Model model, @RequestParam(name = "search", defaultValue = "") String search) {
    logger.info("search for [{}]", search);
    model.addAttribute("search", search);
    return "search-results";
  }

  @GetMapping("/visits/{id}")
  public String visit(Model model, @PathVariable(name = "id") String visitId) {
    logger.info("/visits/{}", visitId);
    model.addAttribute("visitId", visitId);

    List<WebCrawlResult> webCrawlResults = null;
    model.addAttribute("webCrawlResults", webCrawlResults);

    // TODO:  move to repository class
    ObjectMapper objectMapper = new ObjectMapper();
    objectMapper.registerModule(new JavaTimeModule());
    JdbcClient jdbcClient = JdbcClient.create(DuckDataSource.memory());
    // get one row from the parquet file
    Optional<String> json = jdbcClient
            .sql("select to_json(p) from 'web.parquet' p where visitId = ? limit 10")
            .param(visitId)
            .query(String.class)
            .optional();
    if (json.isPresent()) {
      try {
        WebCrawlResult webCrawlResult = objectMapper.readValue(json.get(), WebCrawlResult.class);
        model.addAttribute("webCrawlResults", List.of(webCrawlResult));
        logger.info("webCrawlResult = {}", webCrawlResult);

      } catch (JsonProcessingException e) {
        logger.error(e.getMessage());
      }
    }
    return "visit-details";
  }

}
