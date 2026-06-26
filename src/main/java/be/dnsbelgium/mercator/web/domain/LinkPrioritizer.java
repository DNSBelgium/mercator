package be.dnsbelgium.mercator.web.domain;

public interface LinkPrioritizer {

    default PrioritizedLink prioritize(SiteVisit siteVisit, Page currentPage, Link newLink) {
        double priority = computePriorityFor(siteVisit, currentPage, newLink);
        return new PrioritizedLink(newLink, priority);
    }

    double computePriorityFor(SiteVisit siteVisit, Page currentPage, Link newLink);

}
