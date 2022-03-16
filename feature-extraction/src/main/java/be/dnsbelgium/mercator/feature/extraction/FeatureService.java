package be.dnsbelgium.mercator.feature.extraction;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResult;
import be.dnsbelgium.mercator.content.persistence.ContentCrawlResultRepository;
import be.dnsbelgium.mercator.feature.extraction.metrics.MetricName;
import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeatures;
import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeaturesRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.slf4j.LoggerFactory.getLogger;

@Component
public class FeatureService {

  private static final Logger logger = getLogger(FeatureService.class);
  private final HtmlFeaturesRepository htmlFeaturesRepository;
  private final HtmlFeatureExtractor featureExtractor;
  private final ContentCrawlResultRepository contentCrawlRepository;
  private final HtmlReader htmlReader;
  private final MeterRegistry meterRegistry;

  @Autowired
  public FeatureService(
      MeterRegistry meterRegistry,
      HtmlFeaturesRepository htmlFeaturesRepository,
      HtmlFeatureExtractor featureExtractor,
      ContentCrawlResultRepository contentCrawlRepository,
      HtmlReader htmlReader) {
    this.htmlFeaturesRepository = htmlFeaturesRepository;
    this.contentCrawlRepository = contentCrawlRepository;
    this.htmlReader = htmlReader;
    this.featureExtractor = featureExtractor;
    this.meterRegistry = meterRegistry;
  }

  @Transactional
  public void process(VisitRequest visitRequest) {
     this.process(visitRequest, false);
  }

  @Transactional
  public void process(VisitRequest visitRequest, boolean updateExistingRows) {
    logger.debug("Extracting features for domainName={} and visitId={}, update={}",
        visitRequest.getDomainName(), visitRequest.getVisitId(), updateExistingRows);
    List<ContentCrawlResult> crawlResults = contentCrawlRepository.findSucceededCrawlsByVisitId(visitRequest.getVisitId());
    logger.debug("Found {} crawl results for visitId={}", crawlResults.size(), visitRequest.getVisitId());
    for (ContentCrawlResult crawlResult : crawlResults) {
      if (StringUtils.isEmpty(crawlResult.getHtmlKey())) {
        logger.error("crawlResult with id={} has no html_key => skipping", crawlResult.getId());
      } else {
        UUID visitId = visitRequest.getVisitId();
        String url = crawlResult.getUrl();

        Optional<Long> id = htmlFeaturesRepository.selectIdByVisitIdAndUrl(visitId, url);

        if (id.isPresent() && !updateExistingRows) {
          logger.info("Visit for url={} and visitId={} already processed and update not set => skipping", url, visitId);
          meterRegistry.counter(MetricName.COUNTER_DUPLICATE_REQUESTS).increment();
        } else {
          HtmlFeatures features = extractFeaturesFromHtml(crawlResult);
          if (id.isPresent()) {
            features.id = id.get();
            logger.debug("updating row in feature extraction, domainName={} visitId={}",
                visitRequest.getDomainName(), visitRequest.getVisitId());
            meterRegistry.counter(MetricName.COUNTER_UPDATE_REQUESTS).increment();
          }
          logger.debug("domainName={} visitId={} features={}", visitRequest.getDomainName(), visitRequest.getVisitId(), features);
          // since we don't lock the check above does not guarantee that we will never process the same (visitId,url) simultaneously
          boolean wasDuplicate = htmlFeaturesRepository.saveAndIgnoreDuplicateKeys(features);
          if (wasDuplicate) {
            meterRegistry.counter(MetricName.COUNTER_DUPLICATE_KEYS).increment();
          }
        }
      }
    }
    logger.info("Done extracting features for domainName={} and visitId={}", visitRequest.getDomainName(), visitRequest.getVisitId());
  }

  /**
   * Retrieve HTML and extract features
   * @param crawlResult the ContentCrawlResult to process
   */
  private HtmlFeatures extractFeaturesFromHtml(ContentCrawlResult crawlResult) {
    logger.debug("extractFeaturesFromHtml for {}", crawlResult.getUrl());
    // htmlReader.read will throw an AmazonS3Exception when the bucket or key do not exist

    InputStream inputStream = htmlReader.read(crawlResult);
    try {
      HtmlFeatures features = featureExtractor.extractFromHtml(inputStream, crawlResult.getUrl(), crawlResult.getDomainName());
      features.url = crawlResult.getUrl();
      features.visitId = crawlResult.getVisitId();
      features.crawlTimestamp = ZonedDateTime.now();
      features.domainName = crawlResult.getDomainName();
      logger.debug("HTML features extracted. url={} title={}", features.url, features.title);
      return features;
    } finally {
      close(inputStream, crawlResult.getHtmlKey());
    }
  }

  private void close(InputStream inputStream, String key) {
    try {
      inputStream.close();
    } catch (IOException e) {
      logger.warn("Failed to close input stream for " + key, e);
    }
  }

}
