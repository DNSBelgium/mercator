package be.dnsbelgium.mercator.scheduling;

import be.dnsbelgium.mercator.common.VisitRequest;
import be.dnsbelgium.mercator.metrics.Threads;
import be.dnsbelgium.mercator.visits.MainCrawler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.jms.config.JmsListenerEndpointRegistry;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicInteger;

@Component
public class Worker {

  private static final Logger logger = LoggerFactory.getLogger(Worker.class);

//  private final MainCrawler mainCrawler;
//  private final WorkQueue workQueue;
//
//  private final JmsListenerEndpointRegistry jmsListenerEndpointRegistry;

  /**
   * The JmsListener annotation defines the name of the Destination that this method should listen to
   * and the reference to the JmsListenerContainerFactory to use to create the underlying message listener container.
   * <p>
   * Strictly speaking, that last attribute is not necessary unless you need to customize the way the container is
   * built, as Spring Boot registers a default factory if necessary.
   * <p>
   * [reference documentation](<a href="https://docs.spring.io/spring-framework/reference/integration/jms/annotated.html#jms-annotated-support">...</a>) covers this in more detail.
   */

  private final AtomicInteger messagesReceived = new AtomicInteger();
  private final AtomicInteger failures = new AtomicInteger();

//  public Worker(MainCrawler mainCrawler, WorkQueue workQueue, JmsListenerEndpointRegistry jmsListenerEndpointRegistry) {
//    this.mainCrawler = mainCrawler;
//    this.workQueue = workQueue;
//    this.jmsListenerEndpointRegistry = jmsListenerEndpointRegistry;
//  }
//
//  @JmsListener(destination = "visit_requests", containerFactory = "myFactory", concurrency = "${worker.concurrency:10-30}")
//  public void receiveMessage(VisitRequest visitRequest) {
//    Threads.PROCESS_MESSAGE.incrementAndGet();
//    messagesReceived.incrementAndGet();
//    workQueue.messagePopped();
//    logger.debug("messagesReceived: {}", messagesReceived);
//    logger.debug("** Starting visit on <{}>", visitRequest);
//    try {
//      mainCrawler.visit(visitRequest);
//      logger.debug("** Finished visit on <{}>", visitRequest);
//      workQueue.remove(visitRequest);
//
//    } catch (Exception e) {
//      onFailure(e);
//    } finally {
//      Threads.PROCESS_MESSAGE.decrementAndGet();
//    }
//  }

//  private void onFailure(Exception e) {
//    int failureCount = failures.incrementAndGet();
//    logger.atError()
//            .setMessage("mainCrawler.visit(visitRequest) failed")
//            .setCause(e)
//            .log();
//    if (failureCount > 10) {
//      logger.error("We got over 10 exceptions => stopping the listener until someone looks at the issue.");
//      jmsListenerEndpointRegistry.getListenerContainers().forEach(
//              container -> logger.info("container: {}", container)
//      );
//      jmsListenerEndpointRegistry.stop();
//    }
//
//  }

}

