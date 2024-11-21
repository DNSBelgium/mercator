package be.dnsbelgium.mercator.tls.domain.certificates;

import org.slf4j.Logger;

import javax.net.ssl.X509TrustManager;
import java.security.cert.X509Certificate;

import static org.slf4j.LoggerFactory.getLogger;

public class TrustAnythingTrustManager implements X509TrustManager {

  private static final Logger logger = getLogger(TrustAnythingTrustManager.class);

  public TrustAnythingTrustManager() {
    logger.info("Somebody constructed a TrustAnythingTrustManager: be careful");
  }

  @Override
  public void checkClientTrusted(X509Certificate[] chain, String authType) {
  }

  @Override
  public void checkServerTrusted(X509Certificate[] chain, String authType) {
    // but trust anything you throw at us
    // we could add some logging here, but no need for now
  }

  @Override
  public X509Certificate[] getAcceptedIssuers() {
    return new X509Certificate[0];
  }
}
