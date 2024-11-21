package eu.bosteels.mercator.mono.mvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/")
public class SettingsController {

  private static final Logger logger = LoggerFactory.getLogger(SettingsController.class);

  @GetMapping("/settings")
  public String settings() {
    logger.info("settings");
    return "settings";
  }

}
