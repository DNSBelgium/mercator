package be.dnsbelgium.mercator.web.domain;

import static be.dnsbelgium.mercator.web.domain.Page.getSecondLevelDomainName;

public class ExternalLinkPrioritizer implements LinkPrioritizer {


    @Override
    public double computePriorityFor(SiteVisit siteVisit, Page currentPage, Link link) {
        String landingDomainName = getSecondLevelDomainName(siteVisit.getLandingURL());
        String targetDomain = Page.getSecondLevelDomainName(link.getUrl());

        boolean onHomePage =
                currentPage.getUrl().equals(siteVisit.getLandingURL()) ||
                currentPage.getUrl().equals(siteVisit.getBaseURL());

        // we give external links a higher prio than inner links
        if (targetDomain.equals(landingDomainName)) {
            if (onHomePage) {
                return 0.5;
            } else {
                return 0.0;
            }
        } else {
            if (onHomePage) {
                return 1.0;
            } else {
                return 0.75;
            }
        }
    }
}
