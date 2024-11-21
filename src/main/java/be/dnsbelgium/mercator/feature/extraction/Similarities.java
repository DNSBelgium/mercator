package be.dnsbelgium.mercator.feature.extraction;

import lombok.Getter;
import okhttp3.HttpUrl;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.similarity.LevenshteinDistance;
import org.apache.commons.text.similarity.LongestCommonSubsequence;
import org.slf4j.Logger;

import java.net.IDN;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.slf4j.LoggerFactory.getLogger;

@Getter
public class Similarities {

  private final String title;
  private final String domainName;

  private int editDistance = Integer.MAX_VALUE;
  private int longestCommonSubsequence = 0;
  private float fractionWordsTitleInUrl = 0;
  private int nbWordsTitle = 0;

  private static final Logger logger = getLogger(Similarities.class);

  public static final String SEPARATORS = " ()[]<>-.:|,\t\n\r\f\"'";

  @SuppressWarnings("HttpUrlsUsage")
  public static Similarities fromUrl(String title, String url, int maxTitleLength) {
    if (url == null) {
      return new Similarities(title, null, maxTitleLength);
    }

    String domainName;
    try {
      url = url.startsWith("http://") || url.startsWith("https://") ? url : "http://" + url;
      domainName = (HttpUrl.get(url).host());
    } catch (IllegalArgumentException e) {
      logger.warn("Could not determine domain name for url: [{}], using null domain name", url);
      domainName = null;
    }

    return new Similarities(title, domainName, maxTitleLength);
  }

  public static Similarities fromDomainName(String title, String domainName, int maxTitleLength) {
    return new Similarities(title, domainName, maxTitleLength);
  }

  /**
   * Class to compute similarities metrics between the title of the HTML page and the hostname of the URL
   * Computes the following metrics
   * Edit distance
   * Longest common subsequence
   * Fraction of words in the title that also appear in the hostname
   * Number of distinct words in the title
   * <p>
   * If the title is null, the empty string is used instead
   * If the domainName is null, default values are set for the metrics
   *
   * @param title          Title of the HTML document
   * @param domainName     The domainName or hostName use for comparison
   * @param maxTitleLength Maximum allowed length for the title of the page
   */
  private Similarities(String title, String domainName, int maxTitleLength) {

    String normalizedTitle = StringUtils.normalizeSpace(title);
    String truncatedTitle  = StringUtils.truncate(normalizedTitle, maxTitleLength);
    this.title = (truncatedTitle == null) ? "" : truncatedTitle.toLowerCase(Locale.ROOT);

    if (domainName == null) {
      logger.warn("domainName is null => cannot compute edit distance");
      nbWordsTitle = getWordsInTitle().size();
      this.domainName = null;
      return;
    }
    this.domainName = IDN.toUnicode(domainName);
    computeMetrics();
    logger.debug("{}", this);
  }

  private void computeMetrics() {
    LevenshteinDistance levenshteinDistance = new LevenshteinDistance();
    editDistance = levenshteinDistance.apply(title, domainName);

    LongestCommonSubsequence commonSubsequence = new LongestCommonSubsequence();
    longestCommonSubsequence = commonSubsequence.apply(title, domainName);

    Set<String> wordsInTitle = getWordsInTitle();
    this.nbWordsTitle = wordsInTitle.size();

    int count = 0;
    for (String word : wordsInTitle) {
      if (domainName.contains(word)) {
        count++;
      }
    }

    if (nbWordsTitle > 0) {
      fractionWordsTitleInUrl = (float) count / nbWordsTitle;
    }
  }

  private Set<String> getWordsInTitle() {
    List<String> words = List.of(StringUtils.split(title, SEPARATORS));
    Set<String> wordsInTitle = new HashSet<>(words);
    wordsInTitle.removeIf(String::isBlank);
    return wordsInTitle;
  }

  @Override
  public String toString() {
    return "Similarities{" +
        "title='" + title + '\'' +
        ", domainName='" + domainName + '\'' +
        ", editDistance=" + editDistance +
        ", longestCommonSubsequence=" + longestCommonSubsequence +
        ", fractionWords=" + fractionWordsTitleInUrl +
        ", nbWordsTitle=" + nbWordsTitle +
        '}';
  }
}
