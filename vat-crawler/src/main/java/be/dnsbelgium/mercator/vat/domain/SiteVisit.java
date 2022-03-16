package be.dnsbelgium.mercator.vat.domain;

import lombok.Data;
import okhttp3.HttpUrl;
import org.slf4j.Logger;

import java.util.*;

import static org.slf4j.LoggerFactory.getLogger;

@Data
public class SiteVisit {

  // the URL where the visit started
  private final HttpUrl baseURL;

  // where we arrived after fetching the baseURL
  private HttpUrl finalUrl;

  private final Map<Link, Page> visitedPages = new LinkedHashMap<>();

  private final LinkedHashSet<HttpUrl> visitedURLs = new LinkedHashSet<>();
  private final Map<HttpUrl, PrioritizedLink> todoList = new HashMap<>();

  // URL of Page where a VAT number was found
  private HttpUrl matchingURL;

  // the text of the link that got us to the page where a VAT was found
  private String matchingLinkText;

  private final List<String> vatValues = new ArrayList<>();
  private boolean vatFound = false;

  private static final Logger logger = getLogger(SiteVisit.class);

  public SiteVisit(HttpUrl baseURL) {
    this.baseURL = baseURL;
  }

  public void add(Link link, Page page) {
    if (visitedPages.containsKey(link)) {
      logger.error("This should not happen: visited already contains link {}", link);
    }
    visitedPages.put(link, page);
    visitedURLs.add(page.getUrl());
    visitedURLs.add(link.getUrl());

    // TODO: should we use page or link to mark URL as done ?
    // probably both

    todoList.remove(page.getUrl());
    todoList.remove(link.getUrl());

    if (page.isVatFound()) {
      this.vatFound = true;
      this.vatValues.addAll(page.getVatValues());
      this.matchingURL = page.getUrl();
      logger.debug("VAT found on {} after {} page visits", matchingURL, visitedPages.size());
    }
  }

  public void markVisited(Link link) {
    visitedURLs.add(link.getUrl());
    todoList.remove(link.getUrl());
  }

  public int getNumberOfVisitedPages() {
    return visitedPages.size();
  }

  public int getNumberOfVisitedLinks() {
    return visitedURLs.size();
  }

  public boolean alreadyVisited(HttpUrl url) {
    return visitedURLs.contains(url);
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", SiteVisit.class.getSimpleName() + "[", "]")
        .add("baseURL=" + baseURL)
        .add("numberOfVisitedPages=" + visitedPages.size())
        .toString();
  }

  /**
   * Add a link to the todo_list of this SiteVisit
   * @param linkToVisit the link that has to be visited
   */
  public void addLinkToVisit(PrioritizedLink linkToVisit) {
    HttpUrl url = linkToVisit.getUrl();
    if (visitedURLs.contains(url)) {
      // URL was already visited, nothing to do
      return;
    }
    PrioritizedLink otherLink = todoList.get(url);
    if (otherLink == null) {
      // URL was not yet in our todo_list: simply add it now
      todoList.put(url, linkToVisit);
    } else {
      if (linkToVisit.getPriority() > otherLink.getPriority()) {
        // replace otherLink with linkToVisit since it has higher priority
        todoList.put(url, linkToVisit);
      }
    }
  }

  public Optional<PrioritizedLink> getHighestPriorityLink() {
    return todoList.values().stream().min(PrioritizedLink::compareTo);
  }

}

