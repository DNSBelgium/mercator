package be.dnsbelgium.mercator.geoip;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import lombok.extern.log4j.Log4j2;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.util.Timeout;

import be.dnsbelgium.mercator.wappalyzer.jappalyzer.PageResponse;

@Log4j2
public class DownloadUtil {

  private DownloadUtil() {
  }

  public static Optional<byte[]> getAsBytes(String url, String logUrl, int timeoutInSeconds) {
    log.info("GET URL: {}", logUrl);
    log.info("GET URL: {}", url);

    Timeout timeout = Timeout.ofSeconds(timeoutInSeconds);

    // timeout for waiting during creating of connection
    ConnectionConfig connectionConfig = ConnectionConfig.custom().setConnectTimeout(timeout).build();
    PoolingHttpClientConnectionManager connectionManager = PoolingHttpClientConnectionManagerBuilder
        .create()
        .setDefaultConnectionConfig(connectionConfig)
        .build();

    try (CloseableHttpClient client = HttpClientBuilder
        .create()
        .setConnectionManager(connectionManager)
        .setDefaultRequestConfig(createConfig(timeout))
        .build()) {

      ClassicHttpRequest request = new HttpGet(url);

      HttpClientResponseHandler<Optional<byte[]>> handler = response -> {
        if (response.getCode() == HttpStatus.SC_OK) {
          byte[] bodyBytes = EntityUtils.toByteArray(response.getEntity());
          String content = new String(bodyBytes);
          /*
           * Map<String, List<String>> headers = new HashMap<>();
           * for (org.apache.hc.core5.http.Header header : response.getHeaders()) {
           * headers.computeIfAbsent(header.getName(), k -> new
           * ArrayList<>()).add(header.getValue());
           * }
           * PageResponse pageResponse = new PageResponse(response.getCode(),
           */ // headers, content);

          return Optional.ofNullable(EntityUtils.toByteArray(response.getEntity()));
        } else {
          log.error("GET error: {}", response.getCode());
          log.error("GET error: {}", response.getReasonPhrase());
          return Optional.empty();
        }
      };
      return client.execute(request, handler);

    } catch (IOException e) {
      log.error("Error executing HTTP GET request for: {}", logUrl);
      return Optional.empty();
    }
  }

  private static RequestConfig createConfig(Timeout timeout) {
    return RequestConfig
        .custom()
        .setConnectionRequestTimeout(timeout)
        // socket has timeout, for slow senders
        .setResponseTimeout(timeout)
        // do not let the apache http client initiate redirects
        // build it
        .build();

  }

}