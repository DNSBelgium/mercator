package be.dnsbelgium.mercator.feature.extraction;

import be.dnsbelgium.mercator.feature.extraction.currency.CurrencyDetector;
import be.dnsbelgium.mercator.feature.extraction.currency.CurrencyDetectorResult;
import be.dnsbelgium.mercator.feature.extraction.metrics.MetricName;
import be.dnsbelgium.mercator.feature.extraction.persistence.HtmlFeatures;
import be.dnsbelgium.mercator.feature.extraction.socialmedia.SocialMediaLinkAnalyser;
import io.micrometer.core.instrument.MeterRegistry;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.util.ReplacingInputStream;
import org.jsoup.Jsoup;
import org.jsoup.internal.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static be.dnsbelgium.mercator.common.SurrogateCodePoints.removeIncompleteSurrogates;
import static be.dnsbelgium.mercator.feature.extraction.MercatorLanguageDetector.LanguageSelection;
import static org.slf4j.LoggerFactory.getLogger;

@Component
public class HtmlFeatureExtractor {

  //matches all strings composed of digits, possibly grouped by 3, separated with space dot or comma, ending in dot or comma and two digits
  private final Pattern numeric_pattern = Pattern.compile("\\d+(:?[ .,]\\d{3})*(:?[.,]\\d{2})");

  private static final TagMapper tagMapper = new TagMapper();
  private static final SocialMediaLinkAnalyser socialMediaLinkAnalyser = new SocialMediaLinkAnalyser();
  private static final CurrencyDetector currencyDetector = new CurrencyDetector();
  private final MercatorLanguageDetector mercatorLanguageDetector = new MercatorLanguageDetector();

  private static final Logger logger = getLogger(HtmlFeatureExtractor.class);

  private final int maxBodyTextLength;
  private final int maxMetaTextLength;
  private final int maxTitleLength;
  private final int maxExternalHosts;
  private final int maxLinksSocial;
  private final boolean languageDetectionEnabled;

  private final MeterRegistry meterRegistry;

  public enum LinkType {
    INTERNAL, EXTERNAL, MAILTO, TEL, SMS, FILE, UNKNOWN
  }

  private final static int MAX_LENGTH_HTMLSTRUCT = 2000;

  public HtmlFeatureExtractor(MeterRegistry meterRegistry, boolean languageDetectionEnabled) {
    this.meterRegistry = meterRegistry;
    this.maxBodyTextLength = 20_000;
    this.maxMetaTextLength =  1_000;
    this.maxTitleLength    =  2_000;
    this.maxExternalHosts  =  2_000;
    this.maxLinksSocial    =  10;
    this.languageDetectionEnabled = languageDetectionEnabled;
  }

  @Autowired
  public HtmlFeatureExtractor(
      @Value("${feature.extraction.body.text.maxLength:20000}") int maxBodyTextLength,
      @Value("${feature.extraction.meta.text.maxLength:20000}") int maxMetaTextLength,
      @Value("${feature.extraction.title.text.maxLength:2000}") int maxTitleLength /* maxTitleLength should not exceed the length of the column in the database */,
      @Value("${feature.extraction.max.external.hosts:2000}") int maxExternalHosts,
      @Value("${feature.extraction.max.links.social:10}") int maxLinksSocial,
      @Value("${feature.extraction.languageDetection.enabled:true}") boolean languageDetectionEnabled,
      MeterRegistry meterRegistry) {
    this.maxBodyTextLength = maxBodyTextLength;
    this.maxMetaTextLength = maxMetaTextLength;
    this.maxTitleLength = maxTitleLength;
    this.maxExternalHosts = maxExternalHosts;
    this.maxLinksSocial = maxLinksSocial;
    this.meterRegistry = meterRegistry;
    this.languageDetectionEnabled = languageDetectionEnabled;
    logger.info("languageDetectionEnabled={}", languageDetectionEnabled);
  }

  public HtmlFeatures extractFromHtml(String rawHtml, String url, String domainName) {
    InputStream inputStream = new ByteArrayInputStream(rawHtml.getBytes(StandardCharsets.UTF_8));
    HtmlFeatures htmlFeatures = extractFromHtml(inputStream, url, domainName);
    htmlFeatures.html_length = rawHtml.length();
    htmlFeatures.url = url;
    return htmlFeatures;
  }

  /**
   * Will try to parse the given inputStream as HTML and extract features.
   * Should always return a HtmlFeatures object, even if reading or parsing fails.
   * <p>
   * Note that S3 failures (wrong key or bucket) will usually happen earlier: when opening the InputStream
   * <p>
   * Will determine charset from http-equiv meta tag, if present, or fall back to UTF-8 (which is often safe to do).
   * see <a href="https://jsoup.org/apidocs/org/jsoup/Jsoup.html#parse(java.io.File,java.lang.String,java.lang.String)">
   *     JSoup docs
   *     </a>
   *
   * @param inputStream the stream to read from. Will be closed at the end of this method.
   * @param url The URL where the HTML was retrieved from, to resolve relative links against.
   * @return the extracted html features
   */
  public HtmlFeatures extractFromHtml(InputStream inputStream, String url, String domainName) {
    return extractFromHtml(inputStream, null, url, domainName);
  }

  private Document parse(InputStream inputStream, String charsetName, String url) {
    ReplacingInputStream replacingInputStream = new ReplacingInputStream(inputStream, "\u0000", " ");

    try {
      logger.debug("Parsing HTML with charset={} and url={}", charsetName, url);
      return Jsoup.parse(replacingInputStream, charsetName, url);
    } catch (IOException exception) {
      logger.debug("Failed to parse HTML. url={} exception={}", url, exception.getMessage());
      return null;
    }
  }

  /**
   * Will try to parse the given inputStream as HTML and extract features.
   * Should always return a HtmlFeatures object, even if reading or parsing fails
   * <p>
   * see <a href="https://jsoup.org/apidocs/org/jsoup/Jsoup.html#parse(java.io.File,java.lang.String,java.lang.String)">
   *     parse
   *     </a>
   *
   * @param inputStream the stream to read from. Will be closed at the end of this method.
   * @param charsetName (optional) character set of file contents.
   *                    Set to null to determine from http-equiv meta tag, if present, or fall back to UTF-8 (which is often safe to do).
   * @param url The URL where the HTML was retrieved from, to resolve relative links against.
   * @return the extracted html features
   */
  public HtmlFeatures extractFromHtml(InputStream inputStream, String charsetName, String url, String domainName) {
    HtmlFeatures features = new HtmlFeatures();
    Document document = parse(inputStream, charsetName, url);
    if (document == null) {
      return features;
    }

    features.html_length = document.html().length();
    features.title = document.title();
    if (features.title.length() > maxTitleLength) {
      features.title = StringUtils.abbreviate(features.title, maxTitleLength);
      features.title_truncated = true;
    }
    features.nb_imgs = document.getElementsByTag("img").size();
    processLinks(document, features);

    features.nb_input_txt = document.select("input[type=txt],input[type=search]").size();
    features.nb_button = document.select("button,input[type=button],input[type=submit]").size();

    //the number of words in meta description and meta keyword
    features.meta_text = "";
    try {
      Element metaDescription = document.select("meta[name=description]").first();
      if (metaDescription != null) {
        String metaDescriptionContent = metaDescription.attr("content");
        features.nb_meta_desc = metaDescriptionContent.split(" ").length;
        features.meta_text += metaDescriptionContent + " ";
      }
    } catch (Throwable t) {
      // Catch clause was probably here to catch NPE when no meta tag present. Probably no longer needed
      logger.debug("Failed to process meta description tags: {}", t.toString());
    }
    try {
      Element metaKeywords = document.select("meta[name=keywords]").first();
      if (metaKeywords != null) {
        String mkey = metaKeywords.attr("content");
        features.nb_meta_keyw = mkey.split(" ").length;
        features.meta_text += mkey + " ";
      }
    } catch (Throwable t) {
      // Catch clause was probably here to catch NPE when no meta tag present. Probably no longer needed
      logger.debug("Failed to process meta description keys: {}", t.toString());
    }

    if (features.meta_text.length() > maxMetaTextLength) {
      features.meta_text = StringUtils.abbreviate(features.meta_text, maxMetaTextLength);
      features.meta_text_truncated = true;
    }

    features.nb_tags = document.getAllElements().size();

    features.htmlstruct = tagMapper.compress(document);
    if (features.htmlstruct.length() > MAX_LENGTH_HTMLSTRUCT) {
      logger.info("domainName={} length of htmlstruct = {} exceeds {} => truncating",
              features.htmlstruct.length(), domainName, MAX_LENGTH_HTMLSTRUCT);
      features.htmlstruct = StringUtils.abbreviate(features.htmlstruct, MAX_LENGTH_HTMLSTRUCT);
    }
    String bodyText = document.text();

    processSocialMediaLinks(features, document);

    if (languageDetectionEnabled) {
      detectLanguage(features, bodyText);
    }

    CurrencyDetectorResult currencies = currencyDetector.detect(bodyText);
    features.nb_currency_names = currencies.getNbOccurrences();
    features.nb_distinct_currencies = currencies.getNbDistinct();

    computeSimilarities(features, document, url, domainName);

    processBodyText(features, bodyText);
    return features;
  }


  private void processSocialMediaLinks(HtmlFeatures features, Document document) {
    features.nb_facebook_deep_links = 0;
    features.nb_facebook_shallow_links = 0;
    features.nb_twitter_deep_links = 0;
    features.nb_twitter_shallow_links = 0;
    features.nb_linkedin_deep_links = 0;
    features.nb_linkedin_shallow_links = 0;
    features.nb_vimeo_deep_links = 0;
    features.nb_vimeo_shallow_links = 0;
    features.nb_youtube_deep_links = 0;
    features.nb_youtube_shallow_links = 0;

    List<String> facebookUrls = new ArrayList<>();
    List<String> twitterUrls = new ArrayList<>();
    List<String> linkedinUrls = new ArrayList<>();
    List<String> youtubeUrls = new ArrayList<>();
    List<String> vimeoUrls = new ArrayList<>();

    Elements links = document.select("a[href]");
    for (Element link : links) {
      String url = link.attr("href");

      if (url.endsWith("/")) {
        url = url.substring(0, url.length() - 1);
      }

      switch (socialMediaLinkAnalyser.getType(url)) {
        case FACEBOOK_DEEP:
          if (!facebookUrls.contains(url)) {
            features.nb_facebook_deep_links ++;
            facebookUrls.add(url);
          }
          break;

        case FACEBOOK_SHALLOW:
          if (!facebookUrls.contains(url)) {
            features.nb_facebook_shallow_links ++;
            facebookUrls.add(url);
          }
          break;

        case TWITTER_DEEP:
          if (!twitterUrls.contains(url)) {
            features.nb_twitter_deep_links ++;
            twitterUrls.add(url);
          }
          break;

        case TWITTER_SHALLOW:
          if (!twitterUrls.contains(url)) {
            features.nb_twitter_shallow_links ++;
            twitterUrls.add(url);
          }
          break;

        case LINKEDIN_DEEP:
          if (!linkedinUrls.contains(url)) {
            features.nb_linkedin_deep_links ++;
            linkedinUrls.add(url);
          }
          break;

        case LINKEDIN_SHALLOW:
          if (!linkedinUrls.contains(url)) {
            features.nb_linkedin_shallow_links ++;
            linkedinUrls.add(url);
          }
          break;

        case YOUTUBE_DEEP:
          if (!youtubeUrls.contains(url)) {
            features.nb_youtube_deep_links ++;
            youtubeUrls.add(url);
          }
          break;

        case YOUTUBE_SHALLOW:
          if (!youtubeUrls.contains(url)) {
            features.nb_youtube_shallow_links ++;
            youtubeUrls.add(url);
          }
          break;

        case VIMEO_DEEP:
          if (!vimeoUrls.contains(url)) {
            features.nb_vimeo_deep_links ++;
            vimeoUrls.add(url);
          }
          break;

        case VIMEO_SHALLOW:
          if (!vimeoUrls.contains(url)) {
            features.nb_vimeo_shallow_links ++;
            vimeoUrls.add(url);
          }
          break;

        case UNKNOWN:
          logger.debug("Unknown URL type: {}", url);
          break;
      }
      features.facebook_links = subList(facebookUrls, maxLinksSocial);
      features.twitter_links  = subList(twitterUrls, maxLinksSocial);
      features.linkedin_links = subList(linkedinUrls, maxLinksSocial);
      features.youtube_links  = subList(youtubeUrls, maxLinksSocial);
      features.vimeo_links    = subList(vimeoUrls, maxLinksSocial);
    }

    logger.debug("""
                    Done searching social media links for {}\s
                      Facebook: {} shallow, {} deep
                      Twitter : {} shallow, {} deep
                      Linkedin: {} shallow, {} deep
                      YouTube : {} shallow, {} deep
                      Vimeo   : {} shallow, {} deep""",
        document.baseUri(), features.nb_facebook_shallow_links, features.nb_facebook_deep_links,
        features.nb_twitter_shallow_links, features.nb_twitter_deep_links, features.nb_linkedin_shallow_links,
        features.nb_linkedin_deep_links, features.nb_youtube_shallow_links, features.nb_youtube_deep_links,
        features.nb_vimeo_shallow_links, features.nb_vimeo_deep_links);
  }

  private List<String> subList(List<String> list, int maxSize) {
    int toIndex = Math.min(list.size(), maxSize);
    return new ArrayList<>(list.subList(0, toIndex));
  }


  /**
   * Computes distance metrics between the title tag of the page and the domain name.  To compute those metrics, only
   * alphanumerical characters are kept.
   * <p>
   * Computes the Levenshtein Distance (or edit distance) and the longest common subsequence between the title
   * and the domain name
   *
   * @param features HtmlFeatures object to update with the computed metrics
   * @param document Jsoup document containing the HTML page to compute the metrics for
   * @param url URL where the HTML page was crawled from
   */
  private void computeSimilarities(HtmlFeatures features, Document document, String url, String dn) {
    Similarities similarities;
    String title = document.title();

    similarities = Similarities.fromUrl(title, url, maxTitleLength);
    features.distance_title_final_dn = similarities.getEditDistance();
    features.longest_subsequence_title_final_dn = similarities.getLongestCommonSubsequence();
    features.fraction_words_title_final_dn = similarities.getFractionWordsTitleInUrl();
    features.nb_distinct_words_in_title = similarities.getNbWordsTitle();


    similarities = Similarities.fromDomainName(title, dn, maxTitleLength);
    features.distance_title_initial_dn = similarities.getEditDistance();
    features.longest_subsequence_title_initial_dn = similarities.getLongestCommonSubsequence();
    features.fraction_words_title_initial_dn = similarities.getFractionWordsTitleInUrl();
  }


  public void processBodyText(HtmlFeatures features, String bodyText) {
    StringTokenizer tokenizer = new StringTokenizer(bodyText);
    features.nb_words = tokenizer.countTokens();
    features.nb_letters = bodyText.chars().filter(c -> c!= ' ').count();
    if (bodyText.length() > maxBodyTextLength) {
      logger.debug("body_text has length of {} => abbreviating to {}", bodyText.length(), maxBodyTextLength);
      features.body_text = StringUtils.abbreviate(bodyText, maxBodyTextLength);
      features.body_text_truncated = true;
    } else {
      features.body_text = bodyText;
      features.body_text_truncated = false;
    }
    features.body_text = removeIncompleteSurrogates(features.body_text);
    // We do this after abbreviating the body text since the regex match can take a very long time otherwise
    // (like 10 minutes for one page)
    // the number of strings that look like numbers (see regex at top for explanation)
    Matcher numericMatcher = numeric_pattern.matcher(features.body_text);
    features.nb_numerical_strings = (int) numericMatcher.results().count();
  }

  private void processLinks(Document document, HtmlFeatures features) {
    Elements links = document.select("a[href]");
    Set<String> externalHosts = new LinkedHashSet<>();
    for (Element link : links) {
      String url = link.attr("href");
      String absUrl = link.absUrl("href");
      LinkType linkType = getLinkType(url, document.baseUri());
      switch (linkType) {
        case EXTERNAL:
          features.nb_links_ext++;
          String host = getHost(absUrl);
          if (host != null) {
            externalHosts.add(host);
          }
          break;
        case INTERNAL:
          features.nb_links_int++;
          break;
        case MAILTO:
          features.nb_links_email++;
          break;
        case TEL:
          features.nb_links_tel++;
          break;
        case SMS:
          // TODO: add new column for SMS
          meterRegistry.counter(MetricName.COUNTER_SMS_LINK).increment();
          features.nb_links_tel++;
          break;
        case FILE:
          // TODO: add new columns
          meterRegistry.counter(MetricName.COUNTER_FILE_LINK).increment();
          break;
        case UNKNOWN:
          logger.debug("UNKNOWN link type: url={} absUrl={} baseUri={}", url, absUrl, document.baseUri());
          meterRegistry.counter(MetricName.COUNTER_UNKNOWN_LINK).increment();
          break;
      }
    }
    features.nb_distinct_hosts_in_urls = externalHosts.size();
    features.external_hosts = new ArrayList<>(externalHosts);
    if (maxExternalHosts > 0 && features.external_hosts.size() > maxExternalHosts) {
      features.external_hosts = features.external_hosts.subList(0, maxExternalHosts);
    }
    logger.debug("domainName={} => externalHosts = {}", features.domainName, externalHosts);
  }

  public LinkType getLinkType(String url, String baseUri) {
    String absUrl = StringUtil.resolve(baseUri, url);
    String baseHost = getHost(baseUri);
    return getLinkType(url, absUrl, baseHost);
  }

  /**
   * Determine the type of link
   * @param url       : a URL that may be absolute or relative (or a special url, like tel: or mailto:)
   * @param absUrl    : the url that is made absolute
   * @param baseHost  : the host of the document
   * @return the type of link
   */
  public LinkType getLinkType(String url, String absUrl, String baseHost) {
    if (url.startsWith("tel:")) {
      return LinkType.TEL;
    }
    if (url.startsWith("mailto:")) {
      return LinkType.MAILTO;
    }
    if (url.startsWith("sms:")) {
      return LinkType.SMS;
    }
    if (url.startsWith("file:")) {
      return LinkType.FILE;
    }
    // should we also check for "skype:" links and what not ?
    String host = getHost(absUrl);
    if (host == null && baseHost == null) {
      return LinkType.INTERNAL;
    }
    if (host == null) {
      return LinkType.UNKNOWN;
    }
    // check if they point to the same host, and allow a www prefix on either side
    if (StringUtils.equals(baseHost, host)) {
      return LinkType.INTERNAL;
    }
    if (StringUtils.equals(baseHost, "www." + host)) {
      return LinkType.INTERNAL;
    }
    if (StringUtils.equals("www." + baseHost, host)) {
      return LinkType.INTERNAL;
    }
    if (absUrl.startsWith("http")) {
      return LinkType.EXTERNAL;
    }
    return LinkType.UNKNOWN;
  }

  private String getHost(String url) {
    if (StringUtils.isBlank(url)) {
      return null;
    }
    try {
      URL linkUrl = URI.create(url).toURL();
      return linkUrl.getHost();
    } catch (MalformedURLException e) {
      logger.debug("MalformedURLException for url={} msg={}", url, e.getMessage());
      meterRegistry.counter(MetricName.COUNTER_INVALID_URL).increment();
      return null;
    }  catch (IllegalArgumentException e) {
      logger.debug("IllegalArgumentException for url={} msg={}", url, e.getMessage());
      meterRegistry.counter(MetricName.COUNTER_INVALID_URL).increment();
      return null;
    }

  }

  /**
   * Determine the language(s) of a Jsoup Document using <a href="https://github.com/pemistahl/lingua">lingua</a>
   * Try to detect from the most common languages (all European languages + Arabic, Persian, Japanese, Chinese) and
   * stores the result in body_text_languages
   * Try to detect from all the spoken languages and stores the result in body_text_language_2
   * This strategy allows to detect less common languages as well
   * <p>
   * If the language could not be determined, an empty string "" is stored
   *
   * @param features HtmlFeatures to fill with the detected languages
   * @param bodyText The text of which we want to detect the language
   */
  private void detectLanguage(HtmlFeatures features, String bodyText) {
    logger.debug("Detecting language");
    bodyText = StringUtils.abbreviate(bodyText, maxBodyTextLength);
    features.body_text_language   = mercatorLanguageDetector.detectLanguageOf(bodyText, LanguageSelection.COMMON_LANGUAGES);
    features.body_text_language_2 = mercatorLanguageDetector.detectLanguageOf(bodyText, LanguageSelection.ALL_SPOKEN_LANGUAGES);
  }


  @Override
  public String toString() {
    return new StringJoiner(", ", HtmlFeatureExtractor.class.getSimpleName() + "[", "]")
        .add("numeric_pattern=" + numeric_pattern)
        .add("maxBodyTextLength=" + maxBodyTextLength)
        .add("maxMetaTextLength=" + maxMetaTextLength)
        .add("maxTitleLength=" + maxTitleLength)
        .add("maxExternalHosts=" + maxExternalHosts)
        .toString();
  }

}
