package be.dnsbelgium.mercator.feature.extraction.socialmedia;

import be.dnsbelgium.mercator.feature.extraction.HtmlFeatureExtractor;
import be.dnsbelgium.mercator.feature.extraction.ResourceReader;
import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeatures;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings("HttpUrlsUsage")
public class SocialMediaLinkAnalyserTest {

  private static final SocialMediaLinkAnalyser analyser = new SocialMediaLinkAnalyser();
  private final MeterRegistry meterRegistry = new SimpleMeterRegistry();
  private final HtmlFeatureExtractor featureExtractor = new HtmlFeatureExtractor(meterRegistry);
  private static final String URL = "http://www.example.com";
  private static final String DOMAIN = "example.com";

  @Test
  public void testGetTypeForFacebookDeepLinks() {
    List<String> urls = List.of(
        "http://facebook.com/not-a-shallow-link-2",
        "www.facebook.com/some.user",
        "www.facebook.com/100000357",
        "www.facebook.com/profile.php?id=19990357"
    );
    assertThatLinksAre(urls, SocialMediaLinkType.FACEBOOK_DEEP);
  }

  @Test
  public void testGetTypeForFacebookShallowLinks() {
    List<String> urls = List.of(
        "facebook.com",
        "https://facebook.com",
        "facebook.com/marketplace",
        "facebook.com/profile.php",
        "www.facebook.com",
        "www.facebook.com/",
        "www.facebook.com/sharer/sharer.php?u=https://www.dnsbelgium.be",
        "https://www.facebook.com/privacy/explanation"
    );
    assertThatLinksAre(urls, SocialMediaLinkType.FACEBOOK_SHALLOW);
  }

  @Test
  public void testGetTypeForTwitterDeepLinks() {
    List<String> urls = List.of(
        "https://twitter.com/karllorey/status/1259924082067374088",
        "http://twitter.com/@karlloreyone",
        "http://twitter.com/karlloreytwo",
        "twitter.com/karlloreythree"
    );
    assertThatLinksAre(urls, SocialMediaLinkType.TWITTER_DEEP);
  }


  @Test
  public void testGetTypeForTwitterShallowLinks() {
    List<String> urls = List.of(
        "https://www.twitter.com",
        "http://twitter.com/",
        "https://twitter.com/intent/tweet/?url=http://www.example.com/"
    );
    assertThatLinksAre(urls, SocialMediaLinkType.TWITTER_SHALLOW);
  }

  @Test
  public void testGetTypeForLinkedinDeepLinks() {
    List<String> urls = List.of(
        "www.linkedin.com/school/université-grenoble-alpes/",
        "https://linkedin.com/company/dash-company.io",
        "https://www.linkedin.com/company/1234567/",
        "https://www.linkedin.com/feed/update/urn:li:activity:6665508550111912345/",
        "https://de.linkedin.com/in/peter-müller-81a8/",
        "https://linkedin.com/in/karllorey",
        "https://linkedin.com/pub/karlloreyone/abc/123/be",
        "https://www.linkedin.com/pub/karlloreybis/abc/123/be"
    );
    assertThatLinksAre(urls, SocialMediaLinkType.LINKEDIN_DEEP);
  }

  @Test
  public void testGetTypeForLinkedinShallowLinks() {
    List<String> urls = List.of(
        "be.linkedin.com/",
        "http://de.linkedin.com"
    );
    assertThatLinksAre(urls, SocialMediaLinkType.LINKEDIN_SHALLOW);
  }

  @Test
  public void testGetTypeForVariousLinks() {
    List<String> urls = List.of(
        "https://google.com/nice-result",
        "www.google.com",
        "https://website.be/facebook.com/profile.php"
    );
    assertThatLinksAre(urls, SocialMediaLinkType.UNKNOWN);
  }

  @Test
  public void testGetTypeForYoutubeDeepLinks() {
    List<String> urls = List.of(
        "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        "https://youtu.be/dQw4w9WgXcQ",
        "https://youtube.com/embed/dQw4w9WgXcQ",
        "https://youtube.com/watch?v=6_b7RDuLwcI",
        "https://www.youtube.com/user/JPPGmbH",
        "https://www.youtube.com/channel/UC3y00Z1zFPc-8Z9xg8ydC-A",
        "https://www.youtube.com/channel/UCtAh1m085QkEKYNg0j_6r8A",
        "https://www.youtube.com/playlist?list=OLAK5uy_kWUaHsLRRrwSGVbEns1XRsY3CNWxjjzs4",
        "https://nl.youtube.com/playlist?list=OLAK5uy_kWUaHsLRRrwSGVbEns1XRsY3CNWxjjzs4",
        "https://www.youtube.com/c/AcroYogaAntwerp",
        "https://www.youtube.com/AcroYogaAntwerp"
    );

    assertThatLinksAre(urls, SocialMediaLinkType.YOUTUBE_DEEP);
  }

  @Test
  public void testGetTypeForYoutubeShallowLinks() {
    List<String> urls = List.of(
        "https://www.youtube.com/",
        "https://music.youtube.com/",
        "https://music.youtube.com/explore",
        "https://www.youtube.com/feed/history",
        "https://www.youtube.com/gaming",
        "https://www.youtube.com/results?search_query=queen",
        "https://nl.youtube.com",
        "https://youtu.be",
        "https://youtu.be",
        "http://youtu.be"
    );

    assertThatLinksAre(urls, SocialMediaLinkType.YOUTUBE_SHALLOW);
  }

  @Test
  public void testGetTypeForVimeoDeepLinks() {
    List<String> urls = List.of(
        "https://vimeo.com/groups/animation",
        "https://vimeo.com/groups/animation/videos/478481257",
        "https://vimeo.com/user116720514",
        "https://vimeo.com/channels/staffpicks/473446147",
        "https://www.vimeo.com/617054109",
        "https://player.vimeo.com/617054109",
        "https://vimeo.com/channels/music",
        "https://vimeo.com/channels/music/176795301?autoplay=1"
    );

    assertThatLinksAre(urls, SocialMediaLinkType.VIMEO_DEEP);
  }

  @Test
  public void testGetTypeForVimeoShallowLinks() {
    List<String> urls = List.of(
        "https://vimeo.com/channels",
        "https://vimeo.com/features/livestreaming",
        "https://vimeo.com/upgrade",
        "https://vimeo.com/join",
        "https://vimeo.com/",
        "https://vimeo.com/students",
        "https://vimeo.com/create",
        "https://vimeo.com/about",
        "https://vimeo.com/upload",
        "https://vimeo.com/features/video-player",
        "https://vimeo.com/search?q=music"
    );

    assertThatLinksAre(urls, SocialMediaLinkType.VIMEO_SHALLOW);
  }

  @Test
  public void testGetTypeForUnknownLinks() {
    List<String> urls = List.of(
        "https://www.ovh.co.uk/support/contracts/",
        "/over-ons",
        "https://domain.ext/path",
        "https://www.google.com/search?q=youtube.com",
        "#news",
        "https://www.google.com/chrome/browser/desktop/index.html",
        "http://windows.microsoft.com/nl-be/internet-explorer/download-ie"
    );

    assertThatLinksAre(urls, SocialMediaLinkType.UNKNOWN);
  }


  @Test
  public void testContentOfSocialMediaLinksList() {
    String html = ResourceReader.readFileToString("classpath:/html/social-media-links.html");
    HtmlFeatures features = featureExtractor.extractFromHtml(html, URL, DOMAIN);

    assertThat(features.twitter_links).containsExactlyInAnyOrderElementsOf(List.of(
        "https://twitter.com/karllorey/status/1259924082067374088", "http://twitter.com/@karlloreyone",
        "http://twitter.com/karlloreytwo", "twitter.com/karlloreythree", "https://www.twitter.com",
        "http://twitter.com", "https://twitter.com/intent/tweet/?url=http://www.example.com"
    ));

    assertThat(features.linkedin_links).containsExactlyInAnyOrderElementsOf(List.of(
        "www.linkedin.com/school/université-grenoble-alpes", "https://linkedin.com/company/dash-company.io",
        "https://www.linkedin.com/company/1234567", "https://www.linkedin.com/feed/update/urn:li:activity:6665508550111912345",
        "https://de.linkedin.com/in/peter-müller-81a8", "https://linkedin.com/in/karllorey",
        "https://linkedin.com/pub/karlloreyone/abc/123/be", "https://www.linkedin.com/pub/karlloreybis/abc/123/be",
        "be.linkedin.com", "http://de.linkedin.com"
    ));

    assertThat(features.facebook_links).containsExactlyInAnyOrderElementsOf(List.of(
        "facebook.com", "https://facebook.com", "facebook.com/marketplace", "facebook.com/profile.php",
        "www.facebook.com", "http://facebook.com/not-a-shallow-link-2", "www.facebook.com/some.user",
        "www.facebook.com/100000357", "www.facebook.com/profile.php?id=19990357"
    ));
  }

  private void assertThatLinksAre(List<String> urls, SocialMediaLinkType expectedLinkType) {
    for (String url : urls) {
      SocialMediaLinkType linkType = analyser.getType(url);
      assertThat(linkType)
          .withFailMessage("expected  URL [%s] to be %s but was %s", url, expectedLinkType, linkType)
          .isEqualTo(expectedLinkType);
    }
  }

}
