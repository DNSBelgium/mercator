package be.dnsbelgium.mercator.web.domain;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

@Service
public class LinkPrioritizer {

  private final Map<String,Double> linkTextWeights = new HashMap<>();
  private final Map<String,Double> linkUrlWeights = new HashMap<>();
  private static final Logger logger = getLogger(LinkPrioritizer.class);

  public PrioritizedLink prioritize(Link link) {
    double priority = computePriorityFor(link);
    return new PrioritizedLink(link, priority);
  }

  /**
   * Computes a priority between 0.0 and 1.0 for the link.
   * Higher priority means higher chance of finding a VAT on that link
   * @param link the Link for which we will compute a score
   * @return the score for the link
   */
  public double computePriorityFor(Link link) {
    String path = link.getUrl().encodedPath().toLowerCase();
    double urlScore  = getMaxScore(path, linkUrlWeights);
    double textScore = getMaxScore(link.getText().toLowerCase(), linkTextWeights);
    return (urlScore + textScore) / 2.0;
  }

  private double getMaxScore(String s, Map<String,Double> weights) {
    double score = 0.0;
    for (Map.Entry<String, Double> entry : weights.entrySet()) {
      Double weight = entry.getValue();
      String keyword = entry.getKey();
      if (s.contains(keyword) && score < weight) {
        score = weight;
      }
    }
    return score;
  }

  @PostConstruct
  public void init() {
    logger.info("Initializing the LinkPrioritizer, with hard-coded values for now");
    // TODO: get these from database or config file after we have crawled a significant number of websites
    // currently based on the ratio of url's with/without a VAT number containing any of these strings
    linkUrlWeights.put("mentions_legales_box.php", 1.000);
    linkUrlWeights.put("algemene-voorwaarden", 0.997);
    linkUrlWeights.put("ressources", 0.857);
    linkUrlWeights.put("mentions", 0.553);
    linkUrlWeights.put("legal", 0.530);
    linkUrlWeights.put("legales", 0.540);
    linkUrlWeights.put("generales", 0.486);
    linkUrlWeights.put("privacyverklaring", 0.391);
    linkUrlWeights.put("conditions", 0.383);
    linkUrlWeights.put("disclaimer", 0.324);
    linkUrlWeights.put("vente", 0.313);
    linkUrlWeights.put("terms", 0.305);
    linkUrlWeights.put("gdpr", 0.300);
    linkUrlWeights.put("algemene", 0.297);
    linkUrlWeights.put("privacybeleid", 0.267);
    linkUrlWeights.put("policies", 0.256);
    linkUrlWeights.put("general", 0.228);
    linkUrlWeights.put("voorwaarden", 0.217);
    linkUrlWeights.put("contact.php", 0.211);
    linkUrlWeights.put("contacteer", 0.200);
    linkUrlWeights.put("confidentialite", 0.184);
    linkUrlWeights.put("policy", 0.171);
    linkUrlWeights.put("privacy", 0.165);
    linkUrlWeights.put("politique", 0.159);
    linkUrlWeights.put("contact", 0.553);
    linkUrlWeights.put("cookie", 0.039);
    linkUrlWeights.put("about", 0.039);
    linkUrlWeights.put("info", 0.027);
    linkUrlWeights.put("over", 0.019);
    linkUrlWeights.put("ons", 0.007);

    linkTextWeights.put("btw", 0.999);
    linkTextWeights.put("vat", 0.999);
    linkTextWeights.put("about us", 0.98);
    linkTextWeights.put("contact-us", 0.85);
    linkTextWeights.put("about", 0.79);
    linkTextWeights.put("algemene voorwaarden", 0.99);
    linkTextWeights.put("over ons", 0.99);
    linkTextWeights.put("privacy", 0.85);
    linkTextWeights.put("address", 0.85);
    linkTextWeights.put("bezoek ons", 0.85);
    linkTextWeights.put("ons", 0.55);
    linkTextWeights.put("policy", 0.75);
    linkTextWeights.put("over", 0.65);
    linkTextWeights.put("contact", 0.65);
    linkTextWeights.put("privacy beleid", 0.85);
    linkTextWeights.put("contacteer ons", 0.7200);
    linkTextWeights.put("disclaimer", 0.7200);
    linkTextWeights.put("voorwaarden", 0.7200);

    // TODO: get more link texts
  }

}
