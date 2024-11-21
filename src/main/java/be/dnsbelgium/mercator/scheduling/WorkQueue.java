package be.dnsbelgium.mercator.scheduling;

import be.dnsbelgium.mercator.common.VisitRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class WorkQueue {

    private final JmsTemplate jmsTemplate;
    private static final Logger logger = LoggerFactory.getLogger(WorkQueue.class);

    // This number reflects the visits that have not yet been popped from the ActiveMQ queue
    // It can thus be lower than visitsInQueue.size() since visits in progress are not included in this counter.
    private final AtomicInteger approximateQueueSize = new AtomicInteger(0);

    // This set holds the visits that are in the queue and not yet done.
    // The size of the set can thus be bigger than approximateQueueSize since this set also includes visits
    // that are in progress.
    private final Set<String> visitsInQueue = ConcurrentHashMap.newKeySet();

    public WorkQueue(JmsTemplate jmsTemplate) {
        this.jmsTemplate = jmsTemplate;
    }

    private void logQueueSize() {
        int inQueue = approximateQueueSize.get();
        int todoOrInProgress = visitsInQueue.size();
        int inProgress = todoOrInProgress - inQueue;
        logger.debug("Visits inQueue: {}  visits-Todo-Or-In-Progress: {} In-Progress: {}",
                inQueue, todoOrInProgress, inProgress);
    }

    public void add(VisitRequest visitRequest) {
        if (visitsInQueue.contains(visitRequest.getVisitId())) {
            logger.debug("Visit {} already in the queue, not adding again.", visitRequest.getVisitId());
            return;
        }
        jmsTemplate.convertAndSend("visit_requests", visitRequest);
        approximateQueueSize.incrementAndGet();
        visitsInQueue.add(visitRequest.getVisitId());
        logger.debug("Added visit request to the queue: {}", visitRequest);
        logQueueSize();
    }

    public void remove(VisitRequest visitRequest) {
        String visitId = visitRequest.getVisitId();
        visitsInQueue.remove(visitId);
        logQueueSize();
    }


    public void messagePopped() {
        approximateQueueSize.decrementAndGet();
    }

    public int getApproximateQueueSize() {
        return approximateQueueSize.get();
    }
}
