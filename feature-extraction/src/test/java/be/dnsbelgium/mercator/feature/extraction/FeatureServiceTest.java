package be.dnsbelgium.mercator.feature.extraction;


import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResult;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResultRepository;
import be.dnsbelgium.mercator.feature.extraction.metrics.MetricName;
import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeatures;
import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeaturesRepository;
import com.amazonaws.services.s3.model.AmazonS3Exception;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.io.input.NullInputStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static java.lang.Math.abs;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.slf4j.LoggerFactory.getLogger;

@SpringJUnitConfig({ FeatureService.class, SimpleMeterRegistry.class })
@ActiveProfiles({"local", "test"})
class FeatureServiceTest {

  private static final Logger logger = getLogger(FeatureServiceTest.class);

  @Autowired SimpleMeterRegistry meterRegistry;

  @MockBean HtmlFeaturesRepository htmlFeaturesRepository;
  @MockBean ContentCrawlResultRepository contentCrawlRepository;
  @MockBean HtmlReader htmlReader;
  @MockBean HtmlFeatureExtractor featureExtractor;
  @Autowired FeatureService featureService;

  @AfterEach
  public void after() {
    validateMockitoUsage();
  }

  @Test
  public void testVisitWithOneCrawlResult() {
    UUID visitId = UUID.randomUUID();
    String domainName = randomDomainName();
    HtmlFeatures features = randomHtmlFeatures();
    ContentCrawlResult crawlResult = randomContentCrawlResult(domainName);
    setExpectations(crawlResult, features);
    when(contentCrawlRepository.findSucceededCrawlsByVisitId(visitId)).thenReturn(Lists.newArrayList(crawlResult));
    VisitRequest visitRequest = new VisitRequest(visitId, domainName);
    featureService.process(visitRequest);
    verify(contentCrawlRepository).findSucceededCrawlsByVisitId(visitId);
    verify(htmlFeaturesRepository).saveAndIgnoreDuplicateKeys(features);
  }

  @Test
  public void testVisitWithNoCrawlResults() {
    UUID visitId = UUID.randomUUID();
    when(contentCrawlRepository.findSucceededCrawlsByVisitId(visitId)).thenReturn(Lists.emptyList());
    featureService.process(new VisitRequest(visitId, "abc.be"));
    verify(contentCrawlRepository).findSucceededCrawlsByVisitId(visitId);
    verify(htmlFeaturesRepository, never()).save(any(HtmlFeatures.class));
  }

 @Test
  public void testS3Exception() {
    UUID visitId = UUID.randomUUID();
    String domainName = randomDomainName();
    ContentCrawlResult crawlResult = randomContentCrawlResult(domainName);
    HtmlFeatures features = randomHtmlFeatures();

    when(contentCrawlRepository.findSucceededCrawlsByVisitId(visitId)).thenReturn(Lists.newArrayList(crawlResult));
    when(htmlReader.read(crawlResult)).thenThrow(new AmazonS3Exception("fake"));

    assertThrows(AmazonS3Exception.class, () ->
        featureService.process(new VisitRequest(visitId, domainName)))
    ;
    verify(htmlFeaturesRepository, never()).save(features);
  }

  @Test
  public void testVisitWithMultipleCrawlResults() {
    UUID visitId = UUID.randomUUID();
    String domainName = randomDomainName();

    ContentCrawlResult crawlResult1 = randomContentCrawlResult(domainName);
    ContentCrawlResult crawlResult2 = randomContentCrawlResult(domainName);
    ContentCrawlResult crawlResult3 = randomContentCrawlResult(domainName);

    HtmlFeatures features1 = randomHtmlFeatures();
    HtmlFeatures features2 = randomHtmlFeatures();
    HtmlFeatures features3 = randomHtmlFeatures();

    setExpectations(crawlResult1, features1);
    setExpectations(crawlResult2, features2);
    setExpectations(crawlResult3, features3);

    ArrayList<ContentCrawlResult> results = Lists.newArrayList(crawlResult1, crawlResult2, crawlResult3);
    when(contentCrawlRepository.findSucceededCrawlsByVisitId(visitId)).thenReturn(results);

    logger.info("ContentCrawlResults: {}", results);

    VisitRequest visitRequest = new VisitRequest(visitId, domainName);

    featureService.process(visitRequest);

    verify(contentCrawlRepository).findSucceededCrawlsByVisitId(visitId);
    verify(htmlFeaturesRepository).saveAndIgnoreDuplicateKeys(features1);
    verify(htmlFeaturesRepository).saveAndIgnoreDuplicateKeys(features2);
    verify(htmlFeaturesRepository).saveAndIgnoreDuplicateKeys(features3);
  }

  @Test
  public void testVisitAlreadyProcessed() {
    UUID visitId = UUID.randomUUID();
    String domainName = randomDomainName();

    ContentCrawlResult crawlResult1 = randomContentCrawlResult(domainName);
    ContentCrawlResult crawlResult2 = randomContentCrawlResult(domainName);
    ContentCrawlResult crawlResult3 = randomContentCrawlResult(domainName);

    ArrayList<ContentCrawlResult> results = Lists.newArrayList(crawlResult1, crawlResult2, crawlResult3);
    when(contentCrawlRepository.findSucceededCrawlsByVisitId(visitId)).thenReturn(results);

    HtmlFeatures features1 = randomHtmlFeatures();
    HtmlFeatures features2 = randomHtmlFeatures();
    HtmlFeatures features3 = randomHtmlFeatures();

    setExpectations(crawlResult1, features1);
    setExpectations(crawlResult2, features2);
    setExpectations(crawlResult3, features3);

    when(htmlFeaturesRepository.selectIdByVisitIdAndUrl(visitId, crawlResult1.getUrl())).thenReturn(Optional.of(features1.id));
    when(htmlFeaturesRepository.selectIdByVisitIdAndUrl(visitId, crawlResult3.getUrl())).thenReturn(Optional.of(features3.id));

    when(htmlFeaturesRepository.findByVisitIdAndUrl(visitId, crawlResult1.getUrl())).thenReturn(Optional.of(features1));
    when(htmlFeaturesRepository.findByVisitIdAndUrl(visitId, crawlResult3.getUrl())).thenReturn(Optional.of(features3));

    VisitRequest visitRequest = new VisitRequest(visitId, domainName);
    int before = (int) meterRegistry.counter(MetricName.COUNTER_DUPLICATE_REQUESTS).count();

    featureService.process(visitRequest);

    int after = (int) meterRegistry.counter(MetricName.COUNTER_DUPLICATE_REQUESTS).count();
    assertEquals(after, before + 2);
    verify(htmlFeaturesRepository, never()).saveAndIgnoreDuplicateKeys(features1);
    verify(htmlFeaturesRepository).saveAndIgnoreDuplicateKeys(features2);
    verify(htmlFeaturesRepository, never()).saveAndIgnoreDuplicateKeys(features3);
  }

  @Test
  public void testDuplicateKeyCounterIsIncremented() {
    UUID visitId = UUID.randomUUID();
    String domainName = randomDomainName();
    HtmlFeatures features = randomHtmlFeatures();
    ContentCrawlResult crawlResult = randomContentCrawlResult(domainName);
    setExpectations(crawlResult, features);
    ArrayList<ContentCrawlResult> results = Lists.newArrayList(crawlResult);
    when(contentCrawlRepository.findSucceededCrawlsByVisitId(visitId)).thenReturn(results);
    when(htmlFeaturesRepository.saveAndIgnoreDuplicateKeys(features)).thenReturn(true);
    VisitRequest visitRequest = new VisitRequest(visitId, domainName);
    int before = (int) meterRegistry.counter(MetricName.COUNTER_DUPLICATE_KEYS).count();
    logger.info("before = {}", before);
    featureService.process(visitRequest);
    int after = (int) meterRegistry.counter(MetricName.COUNTER_DUPLICATE_KEYS).count();
    logger.info("after = {}", after);
    assertEquals(before + 1, after);
  }

  @Test
  public void testNoUpdateRowHtmlFeatures() {
    UUID visitId = UUID.randomUUID();
    String domainName = randomDomainName();
    HtmlFeatures features = randomHtmlFeatures();
    ContentCrawlResult crawlResult = randomContentCrawlResult(domainName);
    ArrayList<ContentCrawlResult> results = Lists.newArrayList(crawlResult);
    when(contentCrawlRepository.findSucceededCrawlsByVisitId(visitId)).thenReturn(results);
    when(htmlFeaturesRepository.selectIdByVisitIdAndUrl(visitId, crawlResult.getUrl())).thenReturn(Optional.of(features.id));
    when(htmlFeaturesRepository.findByVisitIdAndUrl(visitId, crawlResult.getUrl())).thenReturn(Optional.of(features));

    VisitRequest visitRequest = new VisitRequest(visitId, domainName);
    featureService.process(visitRequest);

    verifyNoInteractions(featureExtractor);
  }


  private void setExpectations(ContentCrawlResult crawlResult, HtmlFeatures features) {
    // this InputStream is never really accessed
    InputStream inputStream = new NullInputStream();
    when(htmlReader.read(crawlResult)).thenReturn(inputStream);
    when(featureExtractor.extractFromHtml(inputStream, crawlResult.getUrl(), crawlResult.getDomainName())).thenReturn(features);
  }

  protected void setExpectations(ContentCrawlResult crawlResult, HtmlFeatures features, InputStream inputStream) {
    // this InputStream is never really accessed
    when(htmlReader.read(crawlResult)).thenReturn(inputStream);
    when(featureExtractor.extractFromHtml(inputStream, crawlResult.getUrl(), crawlResult.getDomainName())).thenReturn(features);
  }


  protected String randomDomainName() {
    return RandomStringUtils.randomAlphabetic(10).toLowerCase() + ".be";
  }

  protected static HtmlFeatures randomHtmlFeatures() {
    return HtmlFeatures.builder()
        .body_text(RandomStringUtils.randomAlphabetic(20))
        .title("Our title is " + RandomStringUtils.randomAlphabetic(5))
        .nb_numerical_strings(RandomUtils.nextInt(0, 20))
        .nb_tags(RandomUtils.nextInt(10, 100))
        .id(abs(new Random().nextLong()))
        .build();
  }

  public static ContentCrawlResult randomContentCrawlResult(String domainName) {
    String url = "https://" + RandomStringUtils.randomAlphabetic(5).toLowerCase() + "." + domainName;
    ContentCrawlResult contentCrawlResult =
        new ContentCrawlResult(UUID.randomUUID(),domainName, url, true, null);
    contentCrawlResult.setBucket(RandomStringUtils.randomAlphabetic(10));
    contentCrawlResult.setHarKey(RandomStringUtils.randomAlphabetic(10) +  ".har");
    contentCrawlResult.setHtmlKey(RandomStringUtils.randomAlphabetic(10) + ".html");
    contentCrawlResult.setHtmlLength(RandomUtils.nextInt(0, 1000));
    contentCrawlResult.setScreenshotKey(RandomStringUtils.randomAlphabetic(10) + ".png");
    contentCrawlResult.setMetricsJson("{}");
    return contentCrawlResult;
  }

  @Test
  public void testUpdateRowHtmlFeatures() {
    UUID visitId = UUID.randomUUID();
    String domainName = randomDomainName();
    InputStream inputStream = new NullInputStream();
    HtmlFeatures features = randomHtmlFeatures();
    ContentCrawlResult crawlResult = randomContentCrawlResult(domainName);
    setExpectations(crawlResult, features, inputStream);
    ArrayList<ContentCrawlResult> results = Lists.newArrayList(crawlResult);
    when(contentCrawlRepository.findSucceededCrawlsByVisitId(visitId)).thenReturn(results);
    when(htmlFeaturesRepository.selectIdByVisitIdAndUrl(visitId, crawlResult.getUrl())).thenReturn(Optional.of(features.id));
    when(htmlFeaturesRepository.findByVisitIdAndUrl(visitId, crawlResult.getUrl())).thenReturn(Optional.of(features));

    int updatesBefore = (int) meterRegistry.counter(MetricName.COUNTER_UPDATE_REQUESTS).count();
    VisitRequest visitRequest = new VisitRequest(visitId, domainName);
    int invocations = 7;
    logger.debug("Sending visit request {} more times", invocations);
    for (int i = 0; i < invocations; i++) {
      featureService.process(visitRequest, true);
    }

    verify(featureExtractor, times(invocations)).extractFromHtml(inputStream, crawlResult.getUrl(), crawlResult.getDomainName());
    int updatesAfter = (int) meterRegistry.counter(MetricName.COUNTER_UPDATE_REQUESTS).count();
    assertEquals(updatesBefore + invocations, updatesAfter);
  }

}