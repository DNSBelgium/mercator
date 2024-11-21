package eu.bosteels.mercator.mono.visits;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.dns.domain.DnsCrawlResult;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Getter
public class VisitResult {

    VisitRequest visitRequest;
    DnsCrawlResult dnsCrawlResult;

    Map<CrawlerModule<?>, List<?>> collectedData;

}
