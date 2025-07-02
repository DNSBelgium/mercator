package be.dnsbelgium.mercator.web.domain;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import okhttp3.HttpUrl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;

import java.io.IOException;


import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

public class MyWireMockTest {

  private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final PageFetcher pageFetcher = new PageFetcher(meterRegistry, PageFetcherConfig.defaultConfig());
  private static final Logger logger = getLogger(MyWireMockTest.class);

  @RegisterExtension
  static WireMockExtension wireMock = WireMockExtension.newInstance()
          .options(wireMockConfig().dynamicPort())
          .build();

  private HttpUrl url(String path) {
    String url = String.format("http://localhost:%d%s", wireMock.getPort(), path);
    return HttpUrl.get(url);
  }

  @Test
  public void test404() throws IOException {
    String path = "/the404.html";
    String body = "<html><h1>Hello world</h1></html>";
    wireMock.stubFor(get(path).willReturn(aResponse().withStatus(404).withBody(body)));
    Page page = pageFetcher.fetch(url(path));
    logger.info("page = {}", page);
    logger.info("page.statusCode = {}", page.getStatusCode());
    assertThat(page.getStatusCode()).isEqualTo(404);
    assertThat(page.getDocument().text()).isEqualTo("Hello world");
  }

}
