package be.dnsbelgium.mercator.ssl.crawler.persistence;

import javax.persistence.Embeddable;
import java.io.Serializable;

@Embeddable
public class CheckAgainstTrustStoreCompositeId implements Serializable {
  private Long certificateDeploymentId;
  private Long trustStoreId;
}
