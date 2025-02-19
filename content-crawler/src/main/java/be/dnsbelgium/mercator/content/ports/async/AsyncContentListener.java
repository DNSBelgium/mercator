package be.dnsbelgium.mercator.content.ports.async;

import be.dnsbelgium.mercator.content.ports.async.model.MuppetsResponseMessage;
import be.dnsbelgium.mercator.content.ports.async.model.ResponseMessage;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;

@Component
public class AsyncContentListener {

  private static final Logger logger = LoggerFactory.getLogger(AsyncContentListener.class);

  private final MuppetsResolutionListener muppetsResolutionListener;

  public AsyncContentListener(MuppetsResolutionListener muppetsResolutionListener) {
    this.muppetsResolutionListener = muppetsResolutionListener;
  }

   @JmsListener(
          destination = "${content.resolving.responseQueues.muppets}",
          containerFactory = "muppetsJmsListenerContainerFactory")
   @Transactional
   public void contentResolved(MuppetsResponseMessage message) throws  JsonProcessingException {
     handleMessage(message, muppetsResolutionListener);
   }

  private <T extends ResponseMessage> void handleMessage(T message, ContentResolutionListener<T> contentResolutionListener) throws JsonProcessingException {
    logger.debug("Received message {} on queue ", message);
    try {
      contentResolutionListener.contentResolved(message);
    } catch (Exception e) {
      logger.error("handling message {} failed", message);
      logger.error("failed handling message", e);
      // throw exception to let DLQ behavior kick in when needed
      throw e;
    }
  }

}
