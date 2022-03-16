package be.dnsbelgium.mercator.geoip;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 * Copied from https://github.com/SIDN/entrada
 */
public class DownloadUtil {

  private static final Logger log = LoggerFactory.getLogger(DownloadUtil.class);

  private DownloadUtil() {
  }

  public static Optional<byte[]> getAsBytes(String url, String logUrl, int timeout) {
    log.info("GET URL: " + logUrl);

    CloseableHttpClient client =
        HttpClientBuilder.create().setDefaultRequestConfig(createConfig(timeout * 1000)).build();
    try {
      HttpResponse response = client.execute(new HttpGet(url));

      if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
        return Optional.ofNullable(EntityUtils.toByteArray(response.getEntity()));
      }
    } catch (Exception e) {
      log.error("Errror executing HTTP GET request for: " + logUrl);
    }

    return Optional.empty();
  }

  private static RequestConfig createConfig(int timeoutMillis) {
    return RequestConfig.custom()
        // timeout for waiting during creating of connection
        .setConnectTimeout(timeoutMillis)
        .setConnectionRequestTimeout(timeoutMillis)
        // socket has timeout, for slow senders
        .setSocketTimeout(timeoutMillis)
        // do not let the apache http client initiate redirects
        .setRelativeRedirectsAllowed(false)
        .setRedirectsEnabled(false)
        .setCircularRedirectsAllowed(false)
        // build it
        .build();
  }

}
