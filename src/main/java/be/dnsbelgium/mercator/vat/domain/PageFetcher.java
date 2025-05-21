package be.dnsbelgium.mercator.vat.domain;

import be.dnsbelgium.mercator.vat.domain.ConfigurableDns.SupportedIpVersion;
import be.dnsbelgium.mercator.vat.metrics.MetricName;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PreDestroy;
import okhttp3.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BoundedInputStream;
import org.apache.commons.io.output.NullOutputStream;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.util.unit.DataSize;

import javax.net.ssl.SSLHandshakeException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;

import static org.slf4j.LoggerFactory.getLogger;

/*
  Uses OkHttp to fetch HTML pages
 */
@SuppressWarnings("resource")
@Service
public class PageFetcher {

  private final static MediaType APPLICATION_JSON = MediaType.parse("application/json");

  private final OkHttpClient client;
  private final Cache cache;
  private final PageFetcherConfig config;
  private final MeterRegistry meterRegistry;

  private static final Logger logger = getLogger(PageFetcher.class);

  public PageFetcher(MeterRegistry meterRegistry, PageFetcherConfig config) {
    logger.info("config = {}", config);
    this.config = config;
    this.meterRegistry = meterRegistry;

    cache = new Cache(config.getCacheDirectory(), config.getCacheSize().toBytes());
    logger.info("cache = {}", cache.directory());
    logger.info("cache.maxSize = {}", cache.maxSize());

    // as long as K8s pods aren't configured for v6, we support v4 only
    Dns dns = new ConfigurableDns(SupportedIpVersion.V4_ONLY);

    this.client = new OkHttpClient.Builder()
            .connectTimeout(config.getConnectTimeOut())
            .writeTimeout(config.getWriteTimeOut())
            .readTimeout(config.getReadTimeOut())
            .callTimeout(config.getCallTimeOut())
            .cache(cache)
            .dns(dns)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .minWebSocketMessageToCompress(1024)
            .connectionSpecs(List.of(ConnectionSpec.COMPATIBLE_TLS, ConnectionSpec.CLEARTEXT))
            .build();
    setupMetrics();
  }

  private void setupMetrics() {
    {
      var cacheSize = new ToDoubleFunction<OkHttpClient>() {
        @Override
        public double applyAsDouble(OkHttpClient httpClient) {
          var cache = httpClient.cache();
          if (cache != null) {
            try {
              return cache.size();
            } catch (IOException e) {
              return 0;
            }
          }
          return 0;
        }
      };
      meterRegistry.gauge(MetricName.COUNTER_OKHTTP_CACHE_SIZE, client, cacheSize);
    }
    {
      var hitCount = new ToDoubleFunction<OkHttpClient>() {
        @Override
        public double applyAsDouble(OkHttpClient httpClient) {
          var cache = httpClient.cache();
          if (cache != null) {
            return cache.hitCount();
          }
          return 0;
        }
      };
      meterRegistry.gauge(MetricName.COUNTER_OKHTTP_CACHE_HIT_COUNT, client, hitCount);
    }
    {
      var networkCount = new ToDoubleFunction<OkHttpClient>() {
        @Override
        public double applyAsDouble(OkHttpClient httpClient) {
          var cache = httpClient.cache();
          if (cache != null) {
            return cache.networkCount();
          }
          return 0;
        }
      };
      meterRegistry.gauge(MetricName.COUNTER_OKHTTP_CACHE_NETWORK_COUNT, client, networkCount);
    }
    {
      var hitRatio = new ToDoubleFunction<OkHttpClient>() {
        @Override
        public double applyAsDouble(OkHttpClient httpClient) {
          var cache = httpClient.cache();
          if (cache != null) {
            return 1.0 * cache.hitCount() / cache.requestCount();
          }
          return 0;
        }
      };
      meterRegistry.gauge(MetricName.COUNTER_OKHTTP_CACHE_HIT_RATIO, client, hitRatio);
    }
  }

  public void clearCache() {
    try {
      @SuppressWarnings("resource")
      Cache cache = client.cache();
      if (cache != null) {
        logger.info("clearing the cache");
        cache.evictAll();
        logger.info("cleared the cache");
      }
    } catch (IOException e) {
      logger.warn("Failed to clear the cache: {}", e.getMessage());
    }
  }

  @PreDestroy
  public void close() {
    logger.info("Closing the PageFetcher");

    logger.info("cache.hitCount = {}", cache.hitCount());
    logger.info("cache.networkCount = {}", cache.networkCount());
    logger.info("cache.requestCount = {}", cache.requestCount());

    try {
      client.dispatcher().executorService().shutdown();
      client.connectionPool().evictAll();
      Cache cache = client.cache();
      if (cache != null) {
        cache.close();
      }
    } catch (IOException e) {
      logger.warn("Error during close", e);
    }
  }

  /**
   * Processes the response body by:
   * - Reading up to a configured maximum number of bytes into memory.
   * - Truncating the body if it exceeds the limit.
   * - Estimating total content length; sets to -1 if it exceeds a hard limit.
   *
   * @param responseBody the HTTP response body
   * @param builder the PageBuilder to populate
   * @param config configuration containing max content length
   * @throws IOException if an I/O error occurs
   */
  public static void handleBody(ResponseBody responseBody, Page.PageBuilder builder, PageFetcherConfig config) throws IOException {

    int maxInMemoryLength = (int) config.getMaxContentLength().toBytes();
    int maxTotalLength = (int) config.getContentLengthDetectionLimit().toBytes();

    long contentLength;
    String body;

    try (InputStream is = responseBody.byteStream();
         ByteArrayOutputStream os = new ByteArrayOutputStream(maxInMemoryLength)) {

      // Read up to maxInMemoryLength into memory
      BoundedInputStream limitedIn = BoundedInputStream.builder()
          .setInputStream(is)
          .setMaxCount(maxInMemoryLength)
          .get();

      long bytesRead = IOUtils.copyLarge(limitedIn, os);

      // Continue reading (without storing) to check total size
      BoundedInputStream fullReadCheck = BoundedInputStream.builder()
          .setInputStream(is)
          .setCount(bytesRead)
          .setMaxCount(maxTotalLength)
          .get();

      long extraBytes = IOUtils.copyLarge(fullReadCheck, new NullOutputStream());

      contentLength = bytesRead + extraBytes;
      if (contentLength == maxTotalLength) {
        contentLength = -1;
      }
      body = os.toString(StandardCharsets.UTF_8);

      builder.responseBody(body)
          .contentLength(contentLength);
    }
  }

  public Page fetch(HttpUrl url) throws IOException {

    Page.PageBuilder builder = Page.builder();

    Instant started = Instant.now();
    String cacheControl = "max-stale=" + config.getCacheMaxStale().toSeconds();
    Request request = new Request.Builder()
            .url(url)
            .header("Cache-Control", cacheControl)
            .header("User-Agent", config.getUserAgent())
            .build();

    logger.debug("request = {}", request);

    try (Response response = client.newCall(request).execute()) {

      if (logger.isDebugEnabled()) {
        debug(response);
      }

      Instant sentRequest = Instant.ofEpochMilli(response.sentRequestAtMillis());
      Instant receivedResponse = Instant.ofEpochMilli(response.receivedResponseAtMillis());

      builder.visitStarted(sentRequest);
      builder.visitFinished(receivedResponse);

      Duration duration = Duration.between(sentRequest, receivedResponse);
      logger.debug("Receiving response took {}", duration);
      meterRegistry.timer(MetricName.TIMER_PAGE_RESPONSE_RECEIVED).record(duration);


      try (ResponseBody responseBody = response.body()) {
        if (responseBody == null) {
          logger.error("response.body == null for url={}", url);
          meterRegistry.counter(MetricName.COUNTER_PAGES_FAILED).increment();
          return Page.failed(url, sentRequest, receivedResponse);
        }
        handleBody(responseBody, builder, config);

        MediaType contentType = responseBody.contentType();
        if (!isSupported(contentType)) {
          logger.debug("Skipping content since type = {}", contentType);
          meterRegistry.counter(MetricName.COUNTER_PAGES_CONTENT_TYPE_NOT_SUPPORTED,
                  "content-type", contentType != null ? contentType.toString() : null).increment();
          return Page.CONTENT_TYPE_NOT_SUPPORTED;
        }

        Duration fetchDuration = Duration.between(started, Instant.now());
        meterRegistry.timer(MetricName.TIMER_PAGE_BODY_FETCHED).record(fetchDuration);
        meterRegistry.counter(MetricName.COUNTER_PAGES_FETCHED).increment();
        if (!url.equals(response.request().url())) {
          logger.debug("Requested {} but received response for {}", url, response.request().url());
        }
        logger.debug("Fetching {} => {} took {}", url, response.request().url(), fetchDuration);

        Page page = builder.url(response.request().url())
            .statusCode(response.code())
            .headers(getHeaders(response))
            .mediaType(responseBody.contentType()).build();

        return page;
      }
    } catch (SSLHandshakeException | ConnectException e) {
      logger.debug("Failed to fetch {} because of {}", url, e.getMessage());
      meterRegistry.counter(MetricName.COUNTER_PAGES_FAILED).increment();
      return Page.failed(request.url(), started, Instant.now());
    }
  }

  @NotNull
  private static Map<String, List<String>> getHeaders(Response response) {
    Map<String, List<String>> headers = new HashMap<>();
    for (String name : response.headers().names()) {
      headers.put(name, response.headers(name));
    }
    return headers;
  }

  protected static boolean isSupported(MediaType contentType) {
    logger.debug("contentType = {}", contentType);
    if (contentType == null) {
      return true;
    }
    logger.debug("contentType: type={} subtype={} charset={}", contentType.type(), contentType.subtype(),
            contentType.charset());
    if (contentType.equals(APPLICATION_JSON)) {
      return true;
    }
    return !contentType.type().equals("image") &&
            !contentType.type().equals("audio") &&
            !contentType.type().equals("video") &&
            !contentType.type().equals("application");
  }

  public void debug(Response response) {
    logger.debug("response = {}", response);
    logger.debug("response.code = {}", response.code());
    logger.debug("response.isSuccessful = {}", response.isSuccessful());
    logger.debug("response.message = {}", response.message());
    logger.debug("response.priorResponse = {}", response.priorResponse());
    logger.debug("response.cacheResponse = {}", response.cacheResponse());
    logger.debug("response.networkResponse = {}", response.networkResponse());
    logger.debug("response.handshake = {}", response.handshake());
    logger.debug("response.protocol = {}", response.protocol());
    logger.debug("response.sentRequestAtMillis = {}", response.sentRequestAtMillis());
    logger.debug("response.receivedResponseAtMillis = {}", response.receivedResponseAtMillis());
    logger.debug("response.body.contentLength = {}", response.body() == null ? 0 : response.body().contentLength());
  }

}
