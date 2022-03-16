package be.dnsbelgium.mercator.feature.extraction;

import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeatures;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.commons.io.input.BrokenInputStream;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RandomUtils;
import org.assertj.core.util.Lists;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.StringTokenizer;
import java.util.concurrent.atomic.AtomicBoolean;

import static be.dnsbelgium.mercator.feature.extraction.HtmlFeatureExtractor.LinkType;
import static org.assertj.core.api.Assertions.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@SuppressWarnings("HttpUrlsUsage")
class HtmlFeatureExtractorTest {

  private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final HtmlFeatureExtractor featureExtractor = new HtmlFeatureExtractor(meterRegistry);
  private static final Logger logger = getLogger(HtmlFeatureExtractorTest.class);
  private static final String URL = "http://www.example.com";
  private static final String DOMAIN = "example.com";

  List<String> protocols = List.of("http://", "https://");
  List<String> urls = List.of(
      "www.abc.be",
      "www.abc.be/",
      "www.abc.be/xx",
      "www.abc.be/xx?yy=zz",
      "abc.be",
      "abc.be/",
      "abc.be/xx",
      "abc.be/xx?yy=zz"
  );

  @Test
  public void emptyDocument() {
    HtmlFeatures features = featureExtractor.extractFromHtml("", URL, null);
    /*
    JSoup turns this into
        <html>
          <head></head>
          <body></body>
        </html>
     */
    logger.info("features = {}", features);
    HtmlFeatures expected = HtmlFeatures.builder()
        .body_text("")
        .title("")
        .meta_text("")
        .htmlstruct("VSm")
        .nb_tags(4)
        .external_hosts(Collections.emptyList())
        .nb_currency_names(0)
        .nb_distinct_currencies(0)
        .nb_facebook_deep_links(0)
        .nb_facebook_shallow_links(0)
        .nb_twitter_deep_links(0)
        .nb_twitter_shallow_links(0)
        .nb_linkedin_deep_links(0)
        .nb_linkedin_shallow_links(0)
        .nb_youtube_deep_links(0)
        .nb_youtube_shallow_links(0)
        .nb_vimeo_deep_links(0)
        .nb_vimeo_shallow_links(0)
        .distance_title_final_dn(15)
        .distance_title_initial_dn(Integer.MAX_VALUE)
        .fraction_words_title_initial_dn(0.f)
        .fraction_words_title_final_dn(0.f)
        .nb_distinct_words_in_title(0)
        .longest_subsequence_title_initial_dn(0)
        .longest_subsequence_title_final_dn(0)
        .body_text_language(null)
        .body_text_language_2(null)
        .build();
    assertThat(features).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  public void basicDocument() {
    HtmlFeatures features = featureExtractor.extractFromHtml("<body>Hello\n\t \rworld</body>", URL, null);
    /*
    JSoup turns this into
        <html>
          <head></head>
          <body>Hello world</body>
        </html>
     */
    logger.info("features = {}", features);
    HtmlFeatures expected = HtmlFeatures.builder()
        .body_text("Hello world")
        .title("")
        .meta_text("")
        .htmlstruct("VSm")
        .nb_tags(4)
        .nb_words(2)
        .nb_letters(10)
        .external_hosts(Collections.emptyList())
        .nb_currency_names(0)
        .nb_distinct_currencies(0)
        .nb_facebook_deep_links(0)
        .nb_facebook_shallow_links(0)
        .nb_twitter_deep_links(0)
        .nb_twitter_shallow_links(0)
        .nb_linkedin_deep_links(0)
        .nb_linkedin_shallow_links(0)
        .nb_youtube_deep_links(0)
        .nb_youtube_shallow_links(0)
        .nb_vimeo_deep_links(0)
        .nb_vimeo_shallow_links(0)
        .body_text_language("en")
        .body_text_language_2("en")
        .distance_title_final_dn(15)
        .distance_title_initial_dn(Integer.MAX_VALUE)
        .fraction_words_title_initial_dn(0.f)
        .fraction_words_title_final_dn(0.f)
        .nb_distinct_words_in_title(0)
        .longest_subsequence_title_initial_dn(0)
        .longest_subsequence_title_final_dn(0)
        .build();
    assertThat(features).usingRecursiveComparison().isEqualTo(expected);
    assertThat(features).isEqualTo(expected);
  }

  @Test
  public void relativeLinksAreInternal() {
    List<String> relativeLinks = List.of(
        "/",
        "/relative",
        "../relative",
        "../relative/",
        "../rel/../relative/",
        "./relative/",
        "./rel/ative/"
    );
    for (String protocol : protocols) {
      for (String url : urls) {
        for (String link : relativeLinks) {
          assertLinkType(link, protocol + url, LinkType.INTERNAL);
    }}}
  }

  @Test
  public void sameHostIsInternal() {
    for (String protocol1 : protocols) {
      for (String protocol2 : protocols) {
        for (String url1 : urls) {
          for (String url2 : urls) {
            assertLinkType(protocol1 + url1, protocol2 + url2, LinkType.INTERNAL);
    }}}}
  }

  @Test
  public void otherHostIsExternal() {
    for (String protocol1 : protocols) {
      for (String protocol2 : protocols) {
        for (String url1 : urls) {
          for (String url2 : urls) {
            url1 = url1.replace("abc", "xyz");
            assertLinkType(protocol1 + url1, protocol2 + url2, LinkType.EXTERNAL);
    }}}}
  }

  @Test
  public void tel() {
    assertLinkType("tel:00123456789", LinkType.TEL);
    assertLinkType("tel:abc",         LinkType.TEL);
    assertLinkType("tel:",            LinkType.TEL);
  }

  @Test
  public void sms() {
    assertLinkType("sms:00123456789", LinkType.SMS);
    assertLinkType("sms:abc",         LinkType.SMS);
    assertLinkType("sms:",            LinkType.SMS);
  }

  @Test
  public void mailto() {
    String baseUri = RandomStringUtils.random(5);
    assertLinkType("mailto:john@example.com", baseUri, LinkType.MAILTO);
    assertLinkType("mailto:john",             baseUri, LinkType.MAILTO);
    assertLinkType("mailto:",                 baseUri, LinkType.MAILTO);
  }

  @Test
  public void fileLinks() {
    String baseUri = RandomStringUtils.random(5);
    assertLinkType("file:/tmp/xx.txt", baseUri, LinkType.FILE);
    assertLinkType("file:/tmp/",       baseUri, LinkType.FILE);
    assertLinkType("file:/tmp",        baseUri, LinkType.FILE);
    assertLinkType("file:/",           baseUri, LinkType.FILE);
    assertLinkType("file:",            baseUri, LinkType.FILE);
    assertLinkType("file:\\",          baseUri, LinkType.FILE);
  }

  private void assertLinkType(String url, LinkType expected) {
    String baseUri = RandomStringUtils.random(5);
    assertLinkType(url, baseUri, expected);
  }

  private void assertLinkType(String url, String baseUri, LinkType expected) {
    LinkType actual = featureExtractor.getLinkType(url, baseUri);
    logger.info("url={} baseUri={} => {}", url, baseUri, actual);
    assertThat(actual).isEqualTo(expected);
  }

  @Test
  public void nullInputStream() {
    HtmlFeatures features = featureExtractor.extractFromHtml(InputStream.nullInputStream(), URL, DOMAIN);
    logger.info("features = {}", features);
    assertThat(features.nb_tags).isEqualTo(4);
    assertThat(features.title).isEmpty();
  }

  @Test
  public void brokenInputStream() {
    HtmlFeatures features = featureExtractor.extractFromHtml(new BrokenInputStream(), URL, DOMAIN);
    logger.info("features = {}", features);
    assertThat(features.nb_tags).isEqualTo(0);
    assertThat(features.title).isNull();
  }

  @Test
  public void inputStreamShouldBeClosed() {
    final AtomicBoolean streamClosed = new AtomicBoolean(false);
    InputStream inputStream = new ByteArrayInputStream("abc".getBytes(StandardCharsets.UTF_8)) {
      @Override
      public void close() throws IOException {
        super.close();
        streamClosed.set(true);
      }
    };
    HtmlFeatures features = featureExtractor.extractFromHtml(inputStream, URL, DOMAIN);
    logger.info("features = {}", features);
    assertThat(features.nb_tags).isEqualTo(4);
    assertThat(features.title).isEmpty();
    logger.info("Stream was closed: {}", streamClosed);
    //noinspection ConstantConditions
    assertThat(streamClosed).isTrue();
  }

  @Test
  public void invalidXHTML() {
    String html = ResourceReader.readFileToString("classpath:/html/notaris.vlaanderen.html");
    HtmlFeatures features = featureExtractor.extractFromHtml(html, "http://notaris.vlaanderen/", "notaris.vlaanderen");
    logger.info("features = {}", features);
    logger.info("features.body_text = {}", features.body_text);
    logger.info("features.meta_text = {}", features.meta_text);
    // unfortunately muppets mutilates the html
    // see https://muppets-dev.crawl042.dc3.be/notaris.vlaanderen/2021/01/25/http/www.notaris.vlaanderen/index.html/6f50b948-11b6-49e7-971b-0db97adf81a5/index.html)
    // It replaces the auto-closing <iframe> tag with an open and close tag and adds some extra tags
    // The root cause is probably that the content is served with a content-type: text/html header while content is xhtml
    // Consequence is that our body_text contains some HTML
    assertThat(features.body_text).isEqualTo("Register.be Parkpage </div> </body> </html>");
    assertThat(features.meta_text).contains("This domain name has been activated for a Register.be customer");
    assertThat(features.meta_text).contains("Parkpage, Coming Soon, Domains, Domain name, Webhosting");
    // now let's parse the original (x)html
    String originalHtml = ResourceReader.readFileToString("classpath:/html/notaris.vlaanderen.original.html");
    HtmlFeatures originalFeatures = featureExtractor.extractFromHtml(originalHtml, "http://notaris.vlaanderen/", "notaris.vlaanderen");
    logger.info("features on original output= {}", originalFeatures);
    assertThat(originalFeatures).isEqualToIgnoringGivenFields(features, "nb_words", "nb_letters",
        "body_text", "body_text_language", "body_text_language_2");
    assertThat(originalFeatures.body_text).isEqualTo("Register.be Parkpage");
  }

  @Test
  public void internalLinks() {
    String html = ResourceReader.readFileToString("classpath:/html/internal-links.html");
    List<String> urls = List.of(
        "http://www.example.com",
        "https://www.example.com",
        "http://example.com",
        "https://example.com",
        "https://example.com/",
        "https://example.com/xyz"
        );
    for (String url : urls) {
      HtmlFeatures features = featureExtractor.extractFromHtml(html, url, DOMAIN);
      assertThat(features.nb_links_int).isEqualTo(11);
      assertThat(features.nb_links_ext).isEqualTo(1);
      logger.info("features.external_hosts = {}", features.external_hosts);
      assertThat(features.external_hosts).hasSize(1);
      assertThat(features.external_hosts).contains("i.am.external");
    }
    HtmlFeatures features = featureExtractor.extractFromHtml(html, "http://example.be", "example.be");
    assertThat(features.nb_links_int).isEqualTo(7);
    assertThat(features.nb_links_ext).isEqualTo(5);
  }

  @Test
  public void title() {
    HtmlFeatures features = featureExtractor.extractFromHtml(
        "<html><head><title>My Title</title><body><p>Hello world</p></body></html>",
        URL, null);
    logger.info("features = {}", features);
    HtmlFeatures expected = HtmlFeatures.builder()
        .body_text("My Title Hello world")
        .title("My Title")
        .meta_text("")
        .htmlstruct("VSóm+")
        .nb_tags(6)
        .nb_words(4)
        .nb_letters("MyTitleHelloworld".length())
        .external_hosts(Collections.emptyList())
        .nb_currency_names(0)
        .nb_distinct_currencies(0)
        .nb_facebook_deep_links(0)
        .nb_facebook_shallow_links(0)
        .nb_twitter_deep_links(0)
        .nb_twitter_shallow_links(0)
        .nb_linkedin_deep_links(0)
        .nb_linkedin_shallow_links(0)
        .nb_youtube_deep_links(0)
        .nb_youtube_shallow_links(0)
        .nb_vimeo_deep_links(0)
        .nb_vimeo_shallow_links(0)
        .distance_title_final_dn(13)
        .distance_title_initial_dn(Integer.MAX_VALUE)
        .fraction_words_title_initial_dn(0.f)
        .fraction_words_title_final_dn(0.f)
        .nb_distinct_words_in_title(2)
        .longest_subsequence_title_initial_dn(0)
        .longest_subsequence_title_final_dn(3)
        .body_text_language("en")
        .body_text_language_2("en")
        .build();
    assertThat(features).isEqualToComparingFieldByField(expected);
  }

  @Test
  public void unixSolutions() {
    String html = ResourceReader.readFileToString("classpath:/html/conference.brussels.html");
    HtmlFeatures features = featureExtractor.extractFromHtml(html, "http://conference.brussels/", null);
    logger.info("features = {}", features);
    HtmlFeatures expected = HtmlFeatures.builder()
        .body_text("conference.brussels")
        .title("conference.brussels")
        .meta_text("")
        .htmlstruct("VSó|mrnnêáòàaY")
        .nb_tags(15)
        .nb_imgs(1)
        .nb_words(1)
        .nb_letters("conference.brussels".length())
        .nb_links_ext(1)
        .nb_distinct_hosts_in_urls(1)
        .external_hosts(Lists.list("www.unix-solutions.be"))
        .nb_currency_names(0)
        .nb_distinct_currencies(0)
        .nb_facebook_deep_links(0)
        .nb_facebook_shallow_links(0)
        .nb_twitter_deep_links(0)
        .nb_twitter_shallow_links(0)
        .nb_linkedin_deep_links(0)
        .nb_linkedin_shallow_links(0)
        .nb_youtube_deep_links(0)
        .nb_youtube_shallow_links(0)
        .nb_vimeo_deep_links(0)
        .nb_vimeo_shallow_links(0)
        .distance_title_final_dn(0)
        .distance_title_initial_dn(Integer.MAX_VALUE)
        .fraction_words_title_initial_dn(0.f)
        .fraction_words_title_final_dn(1.f)
        .nb_distinct_words_in_title(2)
        .longest_subsequence_title_initial_dn(0)
        .longest_subsequence_title_final_dn(19)
        .body_text_language("nl")
        .body_text_language_2("yo")
        .build();
    assertThat(features).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  public void bodyTextIsTruncated() {
    int maxLength = RandomUtils.nextInt(10, 100);
    HtmlFeatureExtractor featureExtractor = new HtmlFeatureExtractor(
        maxLength, 1000, 1000, 2000, 10, true, meterRegistry);
    String html = ResourceReader.readFileToString("classpath:/html/dnsbelgium.be.html");
    HtmlFeatures features = featureExtractor.extractFromHtml(html, "http://dnsbelgium.be/", "dnsbelgium.be");
    logger.info("features = {}", features);
    assertThat(features.body_text.length()).isEqualTo(maxLength);
    assertThat(features.body_text_truncated).isTrue();
    assertThat(features.nb_words).isGreaterThan(400);
  }

  @Test
  public void metaText() {
    HtmlFeatureExtractor featureExtractor = new HtmlFeatureExtractor(meterRegistry);
    String html = ResourceReader.readFileToString("classpath:/html/simple.html");
    HtmlFeatures features = featureExtractor.extractFromHtml(html, "http://simple.be/", "simple.be");
    logger.info("features.meta_text = {}", features.meta_text);
    assertThat(features.meta_text).isEqualTo("description one two three keyword three four five ");
  }

  @Test
  public void titleIsTruncated() {
    HtmlFeatureExtractor featureExtractor = new HtmlFeatureExtractor(10_000, 1000, 10, 2000, 10, true, meterRegistry);
    String html = ResourceReader.readFileToString("classpath:/html/dnsbelgium.be.html");
    HtmlFeatures features = featureExtractor.extractFromHtml(html, "http://dnsbelgium.be/", "dnsbelgium.be");
    logger.info("features = {}", features);
    assertThat(features.title.length()).isEqualTo(10);
    assertThat(features.title_truncated).isTrue();
    assertThat(features.title).isEqualTo("Registr...");
  }

  @Test
  public void metaTextIsTruncated() {
    HtmlFeatureExtractor featureExtractor = new HtmlFeatureExtractor(10,10,10,0,10, true, meterRegistry);
    logger.info("featureExtractor = {}", featureExtractor);
    String html = ResourceReader.readFileToString("classpath:/html/dnsbelgium.be.html");
    HtmlFeatures features = featureExtractor.extractFromHtml(html, "http://dnsbelgium.be/", "dnsbelgium.be");
    logger.info("features = {}", features);
    assertThat(features.meta_text.length()).isEqualTo(10);
    assertThat(features.meta_text_truncated).isTrue();
    assertThat(features.meta_text).isEqualTo("DNS Bel...");
  }

  @Test
  public void emptyBaseUrl() {
    String html = ResourceReader.readFileToString("classpath:/html/dnsbelgium.be.html");
    HtmlFeatures features = featureExtractor.extractFromHtml(html, "", DOMAIN);
    logger.info("features = {}", features);
    assertThat(features.nb_links_ext).isEqualTo(10);
    assertThat(features.nb_links_int).isEqualTo(87);
    assertThat(features.external_hosts).hasSize(5);
    assertThat(features.external_hosts).contains(
        "www.dnsbelgium.be", "twitter.com", "label.anysurfer.be", "www.facebook.com", "www.wieni.be");
  }

  @Test
  public void invalidBaseUrl() {
    String html = ResourceReader.readFileToString("classpath:/html/dnsbelgium.be.html");
    HtmlFeatures features = featureExtractor.extractFromHtml(html, "this is not a url", DOMAIN);
    logger.info("features = {}", features);
    assertThat(features.nb_links_ext).isEqualTo(10);
    assertThat(features.nb_links_int).isEqualTo(87);
    assertThat(features.external_hosts).hasSize(5);
    assertThat(features.external_hosts).contains(
        "www.dnsbelgium.be", "twitter.com", "label.anysurfer.be", "www.facebook.com", "www.wieni.be");
  }

  @Test
  public void optionsOnOneLine() {
    // Unfortunately, JSoup does not treat <option> elements as block elements
    // When there is no whitespace between two <option> tags, the text in them is joined without a space in between
    String html = "<html>" +
        "<h1>h1</h1><h2>h2</h2>" +
        "<select>\n" +
        "    <option>Apple</option>\n" +
        "    <option>Banana</option>\n" +
        "</select>" +
        "</html>";
    String textWithNewLines = Jsoup.parse(html).text();
    logger.info("text = {}", textWithNewLines);
    assertThat(textWithNewLines).isEqualTo("h1 h2 Apple Banana");
    String htmlWithoutNewLines = html.replace("\n", "").replace(" ","");
    String textWithoutNewLines = Jsoup.parse(htmlWithoutNewLines).text();
    logger.info("textWithoutNewLines = {}", textWithoutNewLines);
    assertThat(textWithoutNewLines).isEqualTo("h1 h2AppleBanana");
  }

  // TODO: rework into new test cases
//  @Test
//  public void getSecondLevelDomainName() {
//    assertThat(featureExtractor.getSecondLevelDomainName("http://www.google.be")).isEqualTo("google.be");
//    assertThat(featureExtractor.getSecondLevelDomainName("http://www.café.be")).isEqualTo("café.be");
//    assertThat(featureExtractor.getSecondLevelDomainName("http://www.ελληνικά.be")).isEqualTo("ελληνικά.be");
//    assertThat(featureExtractor.getSecondLevelDomainName("http://www.xyz.xn--rockandros-k7a.be/")).isEqualTo("rockandrosé.be");
//    // blogspot.be is considered an effective TLD by the Public Suffix List
//    assertThat(featureExtractor.getSecondLevelDomainName("http://www.café.blogspot.be/")).isEqualTo("café.blogspot.be");
//    assertThat(featureExtractor.getSecondLevelDomainName("www.no-protocol.be/")).isNull();
//  }
//
//  @Test
//  public void editDistanceBetween() {
//    assertThat(featureExtractor.editDistanceBetween("café", "http://www.café.be")).isEqualTo(3);
//    assertThat(featureExtractor.editDistanceBetween("mijn café", "http://www.mijn-café.be")).isEqualTo(3);
//    assertThat(featureExtractor.editDistanceBetween("mijn groot café", "http://www.mijn-café.be")).isEqualTo(9);
//
//    assertThat(featureExtractor.editDistanceBetween(null, null)).isEqualTo(-1);
//    assertThat(featureExtractor.editDistanceBetween(null, "http://www.google.be")).isEqualTo(-1);
//    assertThat(featureExtractor.editDistanceBetween("my title", null)).isEqualTo(-1);
//    assertThat(featureExtractor.editDistanceBetween("my title", "www.no-protocol.be")).isEqualTo(-1);
//  }


  @Test
  public void testTokenizer() {
    // We should probably use more delimiters in HtmlFeatureExtractor.processBodyText
    String delimiters = " \t\n\r\f.,;!?\"'&‘@";
    String text = "En toen, heel plots, zei hij \"'t moet gaan gedaan zijn!\" en 't was gedaan. Echt knetter-gek!";
    logger.info("text = {}", text);
    StringTokenizer tokenizer = new StringTokenizer(text , delimiters);
    while (tokenizer.hasMoreTokens()) {
      String word = tokenizer.nextToken();
      logger.info("word = {}", word);
    }
  }

  @Test
  public void testFacebookShallowLinksCounter() {
    String html = ResourceReader.readFileToString("classpath:/html/social-media-links.html");
    HtmlFeatures features = featureExtractor.extractFromHtml(html, URL, DOMAIN);

    assertThat(features.nb_facebook_shallow_links).isEqualTo(5);
  }

  @Test
  public void testFacebookDeepLinksCounter() {
    String html = ResourceReader.readFileToString("classpath:/html/social-media-links.html");
    HtmlFeatures features = featureExtractor.extractFromHtml(html, URL, DOMAIN);

    assertThat(features.nb_facebook_deep_links).isEqualTo(4);
  }

  @Test
  public void testLinkedinDeepLinksCounter() {
    String html = ResourceReader.readFileToString("classpath:/html/social-media-links.html");
    HtmlFeatures features = featureExtractor.extractFromHtml(html, URL, DOMAIN);

    assertThat(features.nb_linkedin_deep_links).isEqualTo(8);
  }

  @Test
  public void testLinkedinShallowLinksCounter() {
    String html = ResourceReader.readFileToString("classpath:/html/social-media-links.html");
    HtmlFeatures features = featureExtractor.extractFromHtml(html, URL, DOMAIN);

    assertThat(features.nb_linkedin_shallow_links).isEqualTo(2);
  }

  @Test
  public void testTwitterDeepLinksCounter() {
    String html = ResourceReader.readFileToString("classpath:/html/social-media-links.html");
    HtmlFeatures features = featureExtractor.extractFromHtml(html, URL, DOMAIN);

    assertThat(features.nb_twitter_deep_links).isEqualTo(4);
  }

  @Test
  public void testTwitterShallowLinksCounter() {
    String html = ResourceReader.readFileToString("classpath:/html/social-media-links.html");
    HtmlFeatures features = featureExtractor.extractFromHtml(html, URL, DOMAIN);

    assertThat(features.nb_twitter_shallow_links).isEqualTo(3);
  }

  @Test
  public void testSizeOfSocialMediaLinksList() {
    String html = ResourceReader.readFileToString("classpath:/html/social-media-links.html");
    HtmlFeatures features = featureExtractor.extractFromHtml(html, URL, DOMAIN);

    assertThat(features.twitter_links.size())
        .isEqualTo(Math.min(features.nb_twitter_deep_links + features.nb_twitter_shallow_links,
            featureExtractor.getMaxLinksSocial()));

    assertThat(features.facebook_links.size())
        .isEqualTo(Math.min(features.nb_facebook_deep_links + features.nb_facebook_shallow_links,
            featureExtractor.getMaxLinksSocial()));

    assertThat(features.linkedin_links.size())
        .isEqualTo(Math.min(features.nb_linkedin_deep_links + features.nb_linkedin_shallow_links,
            featureExtractor.getMaxLinksSocial()));
  }

  @Test
  public void testCountUniqueSocialUrlOnly() {
    String html = ResourceReader.readFileToString("classpath:/html/social-media-links-two.html");
    HtmlFeatures features = featureExtractor.extractFromHtml(html, URL, DOMAIN);

    assertThat(features.facebook_links.size()).isEqualTo(new HashSet<>(features.facebook_links).size());
    assertThat(features.nb_facebook_deep_links + features.nb_facebook_shallow_links).isEqualTo(16);
  }

  @Test
  public void testCompareUrlTitle() {
    String html =
        "<html lang=\"en\">\n" +
        "  <head>\n" +
        "    <title>A new page ! 12</title>\n" +
        "  </head>\n" +
        "  <body>\n" +
        "    <p>Some content</p>\n" +
        "  </body>\n" +
        "</html>";

    HtmlFeatures features = featureExtractor.extractFromHtml(html, "https://www.newpage.be/", "newpage.be");
    assertThat(features.distance_title_final_dn).isEqualTo(10);
    assertThat(features.longest_subsequence_title_final_dn).isEqualTo(7);
  }

}