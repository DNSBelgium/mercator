package be.dnsbelgium.mercator.feature.extraction.domain;

import be.dnsbelgium.mercator.feature.extraction.FeatureCrawler;
import be.dnsbelgium.mercator.feature.extraction.FeatureService;
import be.dnsbelgium.mercator.feature.extraction.HtmlFeatureExtractor;
import be.dnsbelgium.mercator.feature.extraction.ResourceReader;
import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeatures;
import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeaturesRepository;
import be.dnsbelgium.mercator.test.PostgreSqlContainer;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.shaded.com.google.common.collect.Lists;

import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.slf4j.LoggerFactory.getLogger;

@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@DataJpaTest
@ActiveProfiles({"local", "test"})
class HtmlFeaturesRepositoryTest {

  @Container
  static PostgreSqlContainer pgsql = PostgreSqlContainer.getInstance();

  // to avoid "No qualifying bean of type ..."
  // How does Spring define which beans to create for a JPA test ??
  @MockBean FeatureCrawler featureCrawler;
  @MockBean FeatureService featureService;

  @Autowired HtmlFeaturesRepository htmlFeaturesRepository;

  private static final Logger logger = getLogger(HtmlFeaturesRepositoryTest.class);

  @DynamicPropertySource
  static void datasourceProperties(DynamicPropertyRegistry registry) {
    pgsql.setDatasourceProperties(registry, "features");
  }

  @Test
  public void save() {
    String meta = htmlFeaturesRepository.getMetaData();
    logger.info("meta = {}", meta);
    HtmlFeatures htmlFeatures = new HtmlFeatures();
    htmlFeatures.visitId = UUID.randomUUID();
    htmlFeatures.crawlTimestamp = ZonedDateTime.now();
    htmlFeatures.body_text = "This is the body text.";
    htmlFeatures.url = "http://www.test.be";
    htmlFeatures.domainName = "test.be";
    htmlFeatures.body_text = "This is a website";
    htmlFeatures.nb_imgs = 10;
    htmlFeatures.nb_input_txt = 11;
    htmlFeatures.nb_button = 12;
    htmlFeatures.nb_links_email = 13;
    htmlFeatures.nb_distinct_hosts_in_urls = 3;
    htmlFeatures.external_hosts = Lists.newArrayList("abc.be", "google.com", "wieni.be");
    HtmlFeatures saved = htmlFeaturesRepository.save(htmlFeatures);
    logger.info("Saved: {}", saved);
    Optional<HtmlFeatures> found = htmlFeaturesRepository.findById(htmlFeatures.id);
    assertThat(found).isPresent();
    logger.info("found.external_hosts = {}", found.get().external_hosts);
    assertThat(found.get()).isEqualToComparingFieldByField(htmlFeatures);
  }

  @Test
  public void findByVisitIdAndUrl() {
    HtmlFeatures features = createHtmlFeatures();
    HtmlFeatures saved = htmlFeaturesRepository.save(features);
    logger.info("Saved: {}", saved);

    Optional<HtmlFeatures> found = htmlFeaturesRepository.findByVisitIdAndUrl(features.visitId, features.url);
    assertThat(found).isPresent();

    Optional<HtmlFeatures> notFound = htmlFeaturesRepository.findByVisitIdAndUrl(features.visitId, features.url.replace("http:", "https:"));
    assertThat(notFound).isNotPresent();
  }

  @Test
  public void findIdByVisitIdAndUrl() {
    HtmlFeatures features = createHtmlFeatures();
    HtmlFeatures saved = htmlFeaturesRepository.save(features);
    logger.info("Saved: {}", saved);

    Optional<Long> found = htmlFeaturesRepository.selectIdByVisitIdAndUrl(features.visitId, features.url);
    assertThat(found).isPresent();

    Optional<Long> notFound = htmlFeaturesRepository.selectIdByVisitIdAndUrl(features.visitId, "https://nothing-here.be");
    assertThat(notFound).isNotPresent();
  }


  private static HtmlFeatures createHtmlFeatures() {
    HtmlFeatures htmlFeatures = new HtmlFeatures();
    htmlFeatures.visitId = UUID.randomUUID();
    htmlFeatures.crawlTimestamp = ZonedDateTime.now();
    htmlFeatures.body_text = "This is the body text.";
    htmlFeatures.url = "http://www.test.be";
    htmlFeatures.domainName = "test.be";
    htmlFeatures.body_text = "This is a website";
    htmlFeatures.nb_imgs = 10;
    htmlFeatures.nb_input_txt = 11;
    htmlFeatures.nb_button = 12;
    htmlFeatures.nb_links_email = 13;
    htmlFeatures.nb_distinct_hosts_in_urls = 3;
    htmlFeatures.external_hosts = Lists.newArrayList("abc.be", "google.com", "wieni.be");
    return htmlFeatures;
  }

  @Test
  public void insertDuplicateVisitAndUrl() {
    HtmlFeatures features1 = createHtmlFeatures();
    htmlFeaturesRepository.save(features1);
    // Inserting a row with the same visitId and url should fail with DataIntegrityViolationException
    HtmlFeatures features2 = createHtmlFeatures();
    features2.visitId = features1.visitId;
    features2.url     = features1.url;
    boolean duplicate = htmlFeaturesRepository.saveAndIgnoreDuplicateKeys(features2);
    assertThat(duplicate).isTrue();
  }

  @Test
  public void otherConstrainstAreStillReported() {
    HtmlFeatures features = createHtmlFeatures();
    // column allows up to 255
    features.url = StringUtils.repeat("a", 300);
    assertThrows(DataIntegrityViolationException.class,
        () -> htmlFeaturesRepository.saveAndIgnoreDuplicateKeys(features));
  }

  @Test
  public void nullByteInHtmlPage() {

    MeterRegistry meterRegistry = new SimpleMeterRegistry();
    HtmlFeatureExtractor featureExtractor = new HtmlFeatureExtractor(meterRegistry);
    HtmlFeatures features;
    String html;

    html = "<html lang=\"en\">\n" +
        "  <head>\n" +
        "    <title>Hello world</title>\n" +
        "  </head>\n" +
        "  <body>\n" +
        "    <p>Hi with null byte \u0000 here</p>\n" +
        "  </body>\n" +
        "</html>";

    features = featureExtractor.extractFromHtml(html, "http://www.example.be/", "example.be");
    features.visitId = UUID.randomUUID();
    features.crawlTimestamp = ZonedDateTime.now();
    features.domainName = "example.be";

    // This will fail if the HTML contains a null byte that is not handled
    htmlFeaturesRepository.saveAndIgnoreDuplicateKeys(features);
  }

}