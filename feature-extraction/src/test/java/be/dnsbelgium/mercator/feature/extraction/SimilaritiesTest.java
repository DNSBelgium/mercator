package be.dnsbelgium.mercator.feature.extraction;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

@SuppressWarnings("HttpUrlsUsage")
class SimilaritiesTest {

  private final int maxTitleLength = 2000;
  private static final Logger logger = getLogger(SimilaritiesTest.class);

  @Test
  public void perfectMatch() {
    Similarities similarities = Similarities.fromUrl("test.be", "http://test.be", maxTitleLength);
    assertThat(similarities.getEditDistance()).isEqualTo(0);
    assertThat(similarities.getLongestCommonSubsequence()).isEqualTo(7);
  }

  @Test
  public void titleMatchesSLD() {
    Similarities similarities = Similarities.fromUrl("test", "http://test.be", maxTitleLength);
    assertThat(similarities.getEditDistance()).isEqualTo(3);
    assertThat(similarities.getLongestCommonSubsequence()).isEqualTo(4);
  }

  @Test
  public void noMatch() {
    String title = "The website title";
    Similarities similarities = Similarities.fromUrl(title, "http://xxx.co.uk", maxTitleLength);
    assertThat(similarities.getEditDistance()).isEqualTo(title.length());
    assertThat(similarities.getLongestCommonSubsequence()).isEqualTo(0);
  }

  @Test
  public void greekTitle() {
    Similarities similarities = Similarities.fromUrl("ελληνικά", "http://greek.be", maxTitleLength);
    assertThat(similarities.getEditDistance()).isEqualTo(8);
    assertThat(similarities.getLongestCommonSubsequence()).isEqualTo(0);
  }

  @Test
  public void greekHostNameAndTitle() {
    Similarities similarities = Similarities.fromUrl("ελληνικά", "http://ελληνικά.be", maxTitleLength);
    assertThat(similarities.getEditDistance()).isEqualTo(3);
    assertThat(similarities.getLongestCommonSubsequence()).isEqualTo(8);
  }

  @Test
  public void greekMixedCase() {
    String title = "ΕΛΛΗΝΙΚΆ-ελληνικά";
    Similarities similarities = Similarities.fromUrl(title, "http://" + title + ".be", maxTitleLength);
    assertThat(similarities.getEditDistance()).isEqualTo(3);
    assertThat(similarities.getLongestCommonSubsequence()).isEqualTo(title.length());
  }

  @Test
  public void idn() {
    Similarities similarities = Similarities.fromUrl("Het café", "http://www.het-café.be", maxTitleLength);
    assertThat(similarities.getEditDistance()).isEqualTo(8);
    assertThat(similarities.getLongestCommonSubsequence()).isEqualTo(7);
    assertThat(similarities.getNbWordsTitle()).isEqualTo(2);
    assertThat(similarities.getFractionWordsTitleInUrl()).isEqualTo(1);
  }

  @Test
  public void aLabel() {
    // xn--ons-caf-hya.be == ons-café.be
    Similarities similarities = Similarities.fromUrl("ons café", "http://www.xn--ons-caf-hya.be", maxTitleLength);
    assertThat(similarities.getEditDistance()).isEqualTo(8);
    assertThat(similarities.getLongestCommonSubsequence()).isEqualTo(7);
    assertThat(similarities.getNbWordsTitle()).isEqualTo(2);
    assertThat(similarities.getFractionWordsTitleInUrl()).isEqualTo(1);
  }

  @Test
  public void testFractionOfWordsInHostname() {
    String title, url;
    Similarities similarities;

    url = "https://best-shop.be";
    title = "The best shop in the world";
    similarities = Similarities.fromUrl(title, url, maxTitleLength);
    assertThat(similarities.getFractionWordsTitleInUrl()).isEqualTo(2 / 5.f);
    assertThat(similarities.getNbWordsTitle()).isEqualTo(5);

    url = "https://my.fantastic.shop.in.brussels.be";
    title = "My Fantastic shop - Brussels";
    similarities = Similarities.fromUrl(title, url, maxTitleLength);
    assertThat(similarities.getFractionWordsTitleInUrl()).isEqualTo(1.0f);
    assertThat(similarities.getNbWordsTitle()).isEqualTo(4);

    url = "https://MYfantasticSHOPinbrussels.be";
    title = "My Fantastic shop - Brussels";
    similarities = Similarities.fromUrl(title, url, maxTitleLength);
    assertThat(similarities.getFractionWordsTitleInUrl()).isEqualTo(1.0f);
    assertThat(similarities.getNbWordsTitle()).isEqualTo(4);

    url = "https://best-shop.uk";
    title = "The best shop in the UK";
    similarities = Similarities.fromUrl(title, url, maxTitleLength);
    assertThat(similarities.getFractionWordsTitleInUrl()).isEqualTo(3 / 5.f);
    assertThat(similarities.getNbWordsTitle()).isEqualTo(5);

    url = "https://friends.be";
    title = "lets be friends";
    similarities = Similarities.fromUrl(title, url, maxTitleLength);
    assertThat(similarities.getFractionWordsTitleInUrl()).isEqualTo(2 / 3.f);
    assertThat(similarities.getNbWordsTitle()).isEqualTo(3);

    url = "https://friends.be";
    title = "";
    similarities = Similarities.fromUrl(title, url, maxTitleLength);
    assertThat(similarities.getFractionWordsTitleInUrl()).isEqualTo(0);
    assertThat(similarities.getNbWordsTitle()).isEqualTo(0);

    url = "https://shop.in.co.uk/from-belgium?id=3456";
    title = "let's shop in the UK";
    similarities = Similarities.fromUrl(title, url, maxTitleLength);
    assertThat(similarities.getFractionWordsTitleInUrl()).isEqualTo(4 / 6.f);
    assertThat(similarities.getNbWordsTitle()).isEqualTo(6);
  }

  @Test
  public void testDigitsInName() {
    String title, url;
    Similarities similarities;

    url = "stubru-top100.be";
    title = "StuBru Top 100";
    similarities = Similarities.fromUrl(title, url, maxTitleLength);
    assertThat(similarities.getFractionWordsTitleInUrl()).isEqualTo(1);
    assertThat(similarities.getNbWordsTitle()).isEqualTo(3);
  }

  @Test
  public void testWithEmptyUrl() {
    String title = "Test title";
    Similarities similarities;

    similarities = Similarities.fromUrl(title, "", maxTitleLength);

    assertThat(similarities.getEditDistance()).isEqualTo(Integer.MAX_VALUE);
    assertThat(similarities.getFractionWordsTitleInUrl()).isEqualTo(0);
    assertThat(similarities.getLongestCommonSubsequence()).isEqualTo(0);
  }

  @Test
  public void testWithNullUrl() {
    String title = "Test title";
    Similarities similarities;

    similarities = Similarities.fromUrl(title, null, maxTitleLength);

    assertThat(similarities.getEditDistance()).isEqualTo(Integer.MAX_VALUE);
    assertThat(similarities.getFractionWordsTitleInUrl()).isEqualTo(0);
    assertThat(similarities.getLongestCommonSubsequence()).isEqualTo(0);
  }

  @Test
  public void nullTitle() {
    Similarities similarities = Similarities.fromUrl(null, "http://www.test.be", maxTitleLength);
    assertThat(similarities.getEditDistance()).isEqualTo("www.test.be".length());
    assertThat(similarities.getLongestCommonSubsequence()).isEqualTo(0);
    assertThat(similarities.getFractionWordsTitleInUrl()).isEqualTo(0);
    assertThat(similarities.getNbWordsTitle()).isEqualTo(0);
    assertThat(similarities.getTitle()).isEqualTo("");
  }

  @Test
  public void longTitle() {
    String title = "This title is 33 characters long";
    Similarities similarities;
    similarities = Similarities.fromUrl(title, "http://this.testcase.be", 4);
    logger.info("similarities = {}", similarities);
    // should only compare "This" with the URL
    assertThat(similarities.getEditDistance()).isEqualTo(".testcase.be".length());
    assertThat(similarities.getFractionWordsTitleInUrl()).isEqualTo(1.0f);
    assertThat(similarities.getLongestCommonSubsequence()).isEqualTo("this".length());
  }

  @Test
  public void invalidURL() {
    Similarities similarities = Similarities.fromUrl(
        "The title",
        "http://a URL should not contain spaces.be", 4);
    logger.info("similarities = {}", similarities);
    assertThat(similarities.getNbWordsTitle()).isGreaterThan(0);
  }

  @Test
  public void tokenize() {
    String title = "een)twee<drie>vier:vijf|zes\tzeven\nacht\rnegen\ftien\"elf[twaalf]dertien(veertien vijftien-zestien.zeventien,achttien";
    String domain = "een.twee.drie.vier.vijf.zes.zeven.acht.negen.tien.elf.twaalf.dertien.veertien.vijftien.zestien.zeventien.achttien";
    Similarities similarities = Similarities.fromDomainName(title, domain, 500);
    logger.info("similarities = {}", similarities);
    assertThat(similarities.getNbWordsTitle()).isEqualTo(18);
    assertThat(similarities.getFractionWordsTitleInUrl()).isEqualTo(1.0f);
  }

  @Test
  public void separators() {
    for (int i=0; i< Similarities.SEPARATORS.length(); i++) {
      String title = "one" + Similarities.SEPARATORS.charAt(i) + "two";
      Similarities similarities = Similarities.fromDomainName(title, "one.two", 500);
      assertThat(similarities.getNbWordsTitle()).isEqualTo(2);
      assertThat(similarities.getFractionWordsTitleInUrl()).isEqualTo(1.0f);
    }
  }

}