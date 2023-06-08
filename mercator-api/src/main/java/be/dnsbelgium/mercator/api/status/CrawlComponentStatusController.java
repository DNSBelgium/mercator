package be.dnsbelgium.mercator.api.status;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.rest.webmvc.BasePathAwareController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static org.springframework.web.bind.annotation.RequestMethod.GET;

@BasePathAwareController
@Slf4j
public class CrawlComponentStatusController {

  private final CrawlComponentStatusService service;

  public CrawlComponentStatusController(CrawlComponentStatusService service) {
    this.service = service;
  }

  @RequestMapping(method = GET, value = "/status/{visitId}")
  public @ResponseBody CrawlComponentStatus getCrawlComponentStatus(@PathVariable UUID visitId) {
    try {
      return service.getCrawlComponentStatus(visitId);
    } catch (ExecutionException | InterruptedException e) {
      log.error("Something wrong happened", e);
      return new CrawlComponentStatus(visitId, false, false, false, false);
    }
  }
}