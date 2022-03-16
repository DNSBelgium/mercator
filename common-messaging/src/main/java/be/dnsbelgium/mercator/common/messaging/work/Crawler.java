package be.dnsbelgium.mercator.common.messaging.work;

import be.dnsbelgium.mercator.common.messaging.dto.VisitRequest;

/**
 * Generic Crawler interface. Each module should have its own implementation of this interface.
 */
public interface Crawler {

  void process(VisitRequest item) throws Exception;

}
