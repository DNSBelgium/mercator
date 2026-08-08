package be.dnsbelgium.mercator.batch;

import be.dnsbelgium.mercator.common.VisitRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.batch.item.ItemProcessor;

public class DelegatingItemProcessor <T> implements ItemProcessor<VisitRequest, T> {

  private final ItemProcessor<VisitRequest, T> delegate;
  private static final Logger logger = LoggerFactory.getLogger(DelegatingItemProcessor.class);

  public DelegatingItemProcessor(ItemProcessor<VisitRequest, T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public T process(VisitRequest visitRequest) throws Exception {
    MDC.put("domainName", visitRequest.getDomainName());
    MDC.put("visitId", visitRequest.getVisitId());
    try {
      return delegate.process(visitRequest);
    } catch (Exception e) {
      logger.atError()
              .setCause(e)
              .setMessage("Unexpected exception while crawling {}")
              .addArgument(visitRequest)
              .log();
      return null;
    } finally {
      MDC.remove("domainName");
      MDC.remove("visitId");
    }
  }
}
