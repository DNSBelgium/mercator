package be.dnsbelgium.mercator.vat.domain;

import be.dnsbelgium.mercator.vat.domain.ConfigurableDns.SupportedIpVersion;
import be.dnsbelgium.mercator.vat.metrics.MetricName;
import io.micrometer.core.instrument.MeterRegistry;
import okhttp3.*;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.slf4j.LoggerFactory.getLogger;

/*
  Uses OkHttp to fetch HTML pages
 */
@Service
public class PageFetcher {

  private final static long MAX_CONTENT_LENGTH = 8 * 1024 * 1024;

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

    // TODO: keep metrics for time outs ?

    cache = new Cache(config.getCacheDirectory(), config.getCacheSize().toBytes());
    logger.info("cache = {}", cache.directory());
    logger.info("cache.maxSize = {}", cache.maxSize());

    // until K8s pods are configred for v6, we support v4 only
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
  }

  public void clearCache() {
    try {
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

  public Page fetch(HttpUrl url) throws IOException {
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
      Duration duration = Duration.between(sentRequest, receivedResponse);
      logger.debug("Receiving response took {}", duration);
      meterRegistry.timer(MetricName.TIMER_PAGE_RESPONSE_RECEIVED).record(duration);

      long millis = response.receivedResponseAtMillis() - response.sentRequestAtMillis();
      logger.debug("millis = {}", millis);

      try (ResponseBody responseBody = response.body()) {
        if (responseBody == null) {
          logger.error("response.body == null for url={}", url);
          meterRegistry.counter(MetricName.COUNTER_PAGES_FAILED).increment();
          return Page.failed(url, sentRequest, receivedResponse);
        }
        MediaType contentType = responseBody.contentType();
        if (!isSupported(contentType)) {
          logger.info("Skipping content since type = {}", contentType);
          meterRegistry.counter(MetricName.COUNTER_PAGES_CONTENT_TYPE_NOT_SUPPORTED,
              "content-type", contentType != null ? contentType.toString() : null).increment();
          return Page.CONTENT_TYPE_NOT_SUPPORTED;
        }
        long contentLength = responseBody.contentLength();
        if (contentLength > MAX_CONTENT_LENGTH) {
          logger.info("url={} => contentLength {} exceeds max content length of {}", url, responseBody.contentLength(), MAX_CONTENT_LENGTH);
          meterRegistry.counter(MetricName.COUNTER_PAGES_TOO_BIG).increment();
          return Page.PAGE_TOO_BIG;
        }
        String body = responseBody.string();
        Duration fetchDuration = Duration.between(started, Instant.now());
        meterRegistry.timer(MetricName.TIMER_PAGE_BODY_FETCHED).record(fetchDuration);
        meterRegistry.counter(MetricName.COUNTER_PAGES_FETCHED).increment();
        if (!url.equals(response.request().url())) {
          logger.debug("Requested {} but received response for {}", url, response.request().url());
        }
        logger.debug("Fetching {} => {} took {}", url, response.request().url(), fetchDuration);
        if (client.cache() != null) {
          meterRegistry.gauge(MetricName.COUNTER_OKHTTP_CACHE_HIT_COUNT, client.cache().hitCount());
          meterRegistry.gauge(MetricName.COUNTER_OKHTTP_CACHE_NETWORK_COUNT, client.cache().networkCount());
          double hitRatio = 1.0 * client.cache().hitCount() / client.cache().requestCount();
          meterRegistry.gauge(MetricName.COUNTER_OKHTTP_CACHE_HIT_RATIO, hitRatio);
        }
        if (body.length() > MAX_CONTENT_LENGTH) {
          logger.info("url={} already fetched but skipped since length {} exceeds max content length of {}", url, body.length(), MAX_CONTENT_LENGTH);
          meterRegistry.counter(MetricName.COUNTER_PAGES_TOO_BIG).increment();
          return Page.PAGE_TOO_BIG;
        }
        return new Page(
            response.request().url(),
            sentRequest, receivedResponse, response.code(), body, responseBody.contentLength(), contentType);
      }

    } catch (SSLHandshakeException | ConnectException e) {
      logger.info("Failed to fetch {} because of {}", url, e.getMessage());
      meterRegistry.counter(MetricName.COUNTER_PAGES_FAILED).increment();
      return Page.failed(request.url(), started, Instant.now());
    }

  }

  protected boolean isSupported(MediaType contentType) {
    logger.debug("contentType = {}", contentType);
    if (contentType == null) {
      return true;
    }
    logger.debug("contentType: type={} subtype={} charset={}", contentType.type(), contentType.subtype(), contentType.charset());
    if (contentType.equals(APPLICATION_JSON)) {
      return true;
    }
    return
        !contentType.type().equals("image") &&
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
