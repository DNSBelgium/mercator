package be.dnsbelgium.mercator.common.messaging.queue;

public interface QueueClient {

  void convertAndSend(String outputQueue, QueueMessage message);

}
