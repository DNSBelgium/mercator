package be.dnsbelgium.mercator.mvc;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class StatusController {

    private static final Logger logger = LoggerFactory.getLogger(StatusController.class);

    @GetMapping("/status")
    public String status() {
        logger.info("/status called");
        return "status";
    }

}
