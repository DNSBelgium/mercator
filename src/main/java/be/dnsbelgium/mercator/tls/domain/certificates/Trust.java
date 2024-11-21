package be.dnsbelgium.mercator.tls.domain.certificates;

import org.slf4j.Logger;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import static org.slf4j.LoggerFactory.getLogger;

public class Trust {

  private static final Logger logger = getLogger(Trust.class);

  public static X509TrustManager trustAnythingTrustManager() {
    return new TrustAnythingTrustManager();
  }

  public static X509TrustManager defaultTrustManager() {
    String algo = TrustManagerFactory.getDefaultAlgorithm();
    TrustManagerFactory trustManagerFactory;
    try {
      trustManagerFactory = TrustManagerFactory.getInstance(algo);
      trustManagerFactory.init((KeyStore) null);
      TrustManager[] trustManagers = trustManagerFactory.getTrustManagers();
      if (trustManagers.length != 1 || !(trustManagers[0] instanceof X509TrustManager)) {
        throw new IllegalStateException("Unexpected default trust managers:" + Arrays.toString(trustManagers));
      }
      return (X509TrustManager) trustManagers[0];

    } catch (NoSuchAlgorithmException e) {
      logger.info("NoSuchAlgorithmException: {}", e.getMessage());
      throw new RuntimeException(e);
    } catch (KeyStoreException e) {
      logger.info("KeyStoreException: {}", e.getMessage());
      throw new RuntimeException(e);
    }
  }

}
