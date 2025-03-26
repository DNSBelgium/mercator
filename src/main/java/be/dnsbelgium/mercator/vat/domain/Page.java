package be.dnsbelgium.mercator.vat.domain;

import be.dnsbelgium.mercator.common.VisitRequest;
import lombok.Builder;
import lombok.Getter;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;

import java.net.HttpCookie;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

/**
 * Represents a parsed HTML document.
 */
@Getter
public class Page {

  // the saved body text will be abbreviated to this length
  private final static int MAX_BODY_TEXT_LENGTH = 20_000;

  public static Page PAGE_TIME_OUT = new Page();
  public static Page PAGE_TOO_BIG = new Page();
  public static Page CONTENT_TYPE_NOT_SUPPORTED = new Page();

  // the URL we retrieved, might be different from the URL we requested (because
  // we follow redirects)
  private final HttpUrl url;

  private final Instant visitStarted;
  private final Instant visitFinished;
  private int statusCode;
  private String responseBody;

  // Returns the number of bytes or -1 if unknown.
  private long contentLength;
  private MediaType mediaType;

  private Document document;

  private List<String> vatValues = new ArrayList<>();

  private static final Logger logger = getLogger(Page.class);

  private Map<String, List<String>> headers = new HashMap<>();

  private final List<String> scriptSources = new LinkedList<>();
  private final Map<String, List<String>> cookies = new HashMap<>();
  private final Map<String, List<String>> metaMap = new HashMap<>();


  // TODO: use this constructor and remember the Link that got us here so that we
  // can build the path that lead to VAT number
  public Page(Link link, Instant visitStarted, Instant visitFinished, int statusCode, String responseBody,
              long contentLength, MediaType mediaType, Map<String, List<String>> headers) {
    this(link.getUrl(), visitStarted, visitFinished, statusCode, responseBody, contentLength, mediaType, headers);
  }

  @Builder
  public Page(HttpUrl url, Instant visitStarted, Instant visitFinished, int statusCode, String responseBody,
              long contentLength, MediaType mediaType, Map<String, List<String>> headers) {
    this.url = url;
    this.visitStarted = visitStarted;
    this.visitFinished = visitFinished;
    this.statusCode = statusCode;
    this.responseBody = responseBody == null ? "" : responseBody;
    this.contentLength = contentLength;
    this.mediaType = mediaType;
    this.headers = mergeHeaders(headers);

    this.document = Jsoup.parse(this.responseBody, url == null? "" : url.toString());

    Elements scripts = document.select("script");
    for (Element script : scripts) {
      String scriptSrc = script.attr("src");
      if (!scriptSrc.isEmpty()) {
        this.scriptSources.add(scriptSrc);
      }
    }

    Elements metas = document.select("meta");
    for (Element meta : metas) {
      String metaName = meta.attr("name");
      String metaContent = meta.attr("content");
      metaMap.putIfAbsent(metaName, new LinkedList<>());
      metaMap.get(metaName).add(metaContent);
    }

    processCookies(this.headers.get("set-cookie"));
    processCookies(this.headers.get("cookie"));
  }

  private static Map<String, List<String>> mergeHeaders(Map<String, List<String>> headers) {
    if (headers == null) {
      return Map.of();
    }
    return headers.entrySet().stream().collect(Collectors.toUnmodifiableMap(
            e -> e.getKey().toLowerCase(),
            Map.Entry::getValue,
            Page::unionLists
    ));
  }

  private static List<String> unionLists(List<String> l1, List<String> l2) {
    return CollectionUtils.union(l1, l2).stream().toList();
  }

  private void processCookies(List<String> cookieValues) {
    if (cookieValues == null)
      return;
    for (String cookieValue : cookieValues) {
      List<HttpCookie> cookies = HttpCookie.parse(cookieValue);
      for (HttpCookie cookie : cookies) {
        this.addCookie(cookie.getName(), cookie.getValue());
      }
    }
  }

  private void addCookie(String name, String value) {
    this.cookies.computeIfAbsent(name, k -> new LinkedList<>());
    this.cookies.get(name).add(value);
  }

  private Page() {
    this.url = null;
    this.visitStarted = Instant.now();
    this.visitFinished = Instant.now();
  }

  private Page(HttpUrl url, Instant visitStarted, Instant visitFinished) {
    this.url = url;
    this.visitStarted = visitStarted;
    this.visitFinished = visitFinished;
  }

  public static Page failed(HttpUrl url, Instant visitStarted, Instant visitFinished) {
    return new Page(url, visitStarted, visitFinished);
  }

  public void setVatValues(List<String> vatValues) {
    this.vatValues = new ArrayList<>(vatValues);
  }

  public boolean isVatFound() {
    return !vatValues.isEmpty();
  }

  public Set<Link> getLinks() {
    Set<Link> linksOnPage = new HashSet<>();
    if (document != null) {
      document.body();
      Elements links = document.body().getElementsByTag("a");
      for (Element element : links) {
        String href = element.absUrl("href");
        if (StringUtils.isNotBlank(href)) {
          try {
            HttpUrl url = HttpUrl.parse(href);
            if (url != null) {
              Link link = new Link(url, element.text(), getUrl());
              linksOnPage.add(link);
            }
          } catch (Exception e) {
            logger.warn("Skipping href={} on {} because of {}", href, getUrl(), e.getMessage());
          }
        } else {
          // we just ignore invalid URL's
          logger.debug("Invalid url: [{}] found on {}", href, getUrl());
        }
      }
    }
    return linksOnPage;
  }

  public Set<Link> getInnerLinks() {
    Set<Link> linksOnPage = getLinks();
    String domainName = getSecondLevelDomainName(url);

    logger.debug("Before filtering: {} links", linksOnPage.size());
    Set<Link> filtered = linksOnPage.stream()
            .filter(s -> Page.getSecondLevelDomainName(s.getUrl()).equals(domainName))
            .filter(s -> !s.getUrl().scheme().startsWith("mailto:"))
            .filter(s -> !s.getUrl().encodedPath().toLowerCase().endsWith(".png"))
            .filter(s -> !s.getUrl().encodedPath().toLowerCase().endsWith(".jpg"))
            .filter(s -> !s.getUrl().encodedPath().toLowerCase().endsWith(".pdf"))
            .collect(Collectors.toSet());
    logger.debug("After filtering: {} links", filtered.size());
    return filtered;
  }

  /**
   * return the second-level domain name based on the host of given URL (last two
   * labels of the domain name)
   * <p>
   * Will return last two bytes when host is an IPv4 address !
   * (Does not really matter since we use this method to find inner links and
   * always compare with a real domain name)
   * <p>
   * Does not take into public suffix list. So
   * getSecondLevelDomainName("bbc.co.uk") => "co.uk"
   *
   * @param url the url to start from
   * @return the second-level domain name based on the host of given URL
   */
  public static String getSecondLevelDomainName(String url) {
    HttpUrl httpUrl = HttpUrl.parse(url);
    if (httpUrl == null) {
      return "";
    }
    return Page.getSecondLevelDomainName(httpUrl);
  }

  public static String getSecondLevelDomainName(HttpUrl url) {
    // TODO use : url.topPrivateDomain();
    String host = url.host();
    String[] labels = host.split("\\.");
    int labelCount = labels.length;
    if (labelCount <= 2) {
      return host;
    }
    return labels[labelCount - 2] + "." + labels[labelCount - 1];
  }

  @Override
  public String toString() {
    if (this == PAGE_TIME_OUT) {
      return "Page.PAGE_TIME_OUT";
    }
    if (this == PAGE_TOO_BIG) {
      return "Page.PAGE_TOO_BIG";
    }
    if (this == CONTENT_TYPE_NOT_SUPPORTED) {
      return "Page.CONTENT_TYPE_NOT_SUPPORTED";
    }
    return new StringJoiner(", ", Page.class.getSimpleName() + "[", "]")
            .add("url=" + url)
            .add("visitFinished=" + visitFinished)
            .add("statusCode=" + statusCode)
            .add("contentLength=" + contentLength)
            .add("mediaType=" + mediaType)
            .add("vatValues=" + vatValues)
            .toString();
  }

  public PageVisit asPageVisit(VisitRequest visitRequest, boolean includeBodyText) {
    String bodyText = null;
    if (includeBodyText && document != null) {
      bodyText = document.body().text();
      if (bodyText.contains("\u0000")) {
        logger.info("Body for {} seems to have binary data => do not save it", url);
        bodyText = "[did not save binary data]";
      }
      if (bodyText.length() > MAX_BODY_TEXT_LENGTH) {
        logger.debug("body_text has length of {} => abbreviating to {} chars", bodyText.length(), MAX_BODY_TEXT_LENGTH);
        bodyText = StringUtils.abbreviate(bodyText, MAX_BODY_TEXT_LENGTH);
      }
    }
    // TODO: add boolean parameter to control saving html
    String html = document != null ? document.html() : null;
    if (html != null) {
      logger.debug("{} => length(html) = {}", url, html.length());
    } else {
      logger.debug("{} => html == null", url);
    }
    return new PageVisit(
            visitRequest.getVisitId(),
            visitRequest.getDomainName(),
            url != null ? StringUtils.abbreviate(url.toString(), 255) : null,
            url != null ? StringUtils.abbreviate(url.encodedPath(), 255) : null,
            visitStarted,
            visitFinished,
            statusCode,
            bodyText,
            html,
            vatValues,
            contentLength,
            headers
            );
  }

}
