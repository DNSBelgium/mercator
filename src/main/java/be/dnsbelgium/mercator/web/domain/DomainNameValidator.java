package be.dnsbelgium.mercator.web.domain;

import okhttp3.HttpUrl;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class DomainNameValidator {

  private static final Logger logger = getLogger(DomainNameValidator.class);

  @SuppressWarnings("HttpUrlsUsage")
  public static boolean isValidDomainName(String domainName) {
    // using the validation done in okhttp3.HttpUrl seems to be the easiest
    try {
      HttpUrl.get("http://" + domainName);
      return true;
    } catch (IllegalArgumentException e) {
      logger.info("e = {}", e.getMessage());
      return false;
    }
  }

}
