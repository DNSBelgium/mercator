package be.dnsbelgium.mercator.mvc;

import be.dnsbelgium.mercator.persistence.DuckDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("SqlDialectInspection")
@Controller
public class QueryController {

  private static final Logger logger = LoggerFactory.getLogger(QueryController.class);

  private final JdbcClient jdbcClient;

  public QueryController() {
    DataSource dataSource = DuckDataSource.memory();
    jdbcClient = JdbcClient.create(dataSource);
  }

  @GetMapping("/query")
 public String submitCrawlForm() {
    return "query";
 }

  @SuppressWarnings("SqlSourceToSinkFlow")
  @GetMapping("/run_query")
  public String runQuery(@RequestParam(name = "query") String query,
                                @RequestParam(name = "maxRows", defaultValue = "1000") int maxRows,
                                Model model) {
    logger.debug("run_query: {}", query);
    String q = "select * from (" + query + ") limit " + maxRows;

    try {
      List<Map<String, Object>> rows = jdbcClient
              .sql(q)
              .query()
              .listOfRows();
      model.addAttribute("rows", rows);
      model.addAttribute("rowsFound", rows.size());
      if (!rows.isEmpty()) {
        model.addAttribute("columns", rows.getFirst().keySet());
      }
      logger.debug("rows found: {}", rows.size());
    } catch (Exception e) {
      model.addAttribute("error", e.getMessage());
      model.addAttribute("rowsFound", 0);
      model.addAttribute("columns", Set.of());
    }
    model.addAttribute("query", query);
    model.addAttribute("maxRows", maxRows);
    return "query";
  }

}
