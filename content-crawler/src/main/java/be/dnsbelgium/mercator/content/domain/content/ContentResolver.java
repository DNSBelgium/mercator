package be.dnsbelgium.mercator.content.domain.content;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;

import java.util.List;

public interface ContentResolver {

  void requestContentResolving(VisitRequest visitRequest, List<String> urlCandidates);

}