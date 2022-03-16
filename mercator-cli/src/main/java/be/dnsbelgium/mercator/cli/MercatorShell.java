package be.dnsbelgium.mercator.cli;

import com.amazonaws.services.sqs.AmazonSQSAsync;
import com.amazonaws.services.sqs.model.*;
import org.jline.utils.AttributedString;
import org.jline.utils.AttributedStyle;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.shell.Availability;
import org.springframework.shell.jline.PromptProvider;
import org.springframework.shell.standard.ShellComponent;
import org.springframework.shell.standard.ShellMethod;
import org.springframework.shell.standard.ShellMethodAvailability;
import org.springframework.shell.standard.ShellOption;

import javax.annotation.PostConstruct;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.slf4j.LoggerFactory.getLogger;

@ShellComponent
public class MercatorShell implements PromptProvider {

  private static final Logger logger = getLogger(MercatorShell.class);

  @Value("${queue.names}")
  private List<String> queueNames = new ArrayList<>();

  @Value("${dispatcher.input.queue.name:}")
  private String dispatcherInputQueue;
  private String dispatcherInputQueueUrl;

  private final AmazonSQSAsync amazonSqs;

  private String destinationQueue;
  private String destinationQueueUrl;

  private final static String VISIT_REQUEST_TEMPLATE = "{\"visitId\": \"${visitId}\", \"domainName\": \"${domain}\"}";

  private final static String ACK_MESSAGE_TEMPLATE = "{\"domainName\": \"${domainName}\", \"crawlerModule\": \"${crawlerModule}\", \"visitId\" : \"${visitId}\" }";

  @SuppressWarnings("HttpUrlsUsage")
  private final static String MUPPETS_REQUEST_TEMPLATE =
      "{\n" +
          "  \"visitId\": \"${visitId}\",\n" +
          "  \"domainName\": \"${domainName}\",\n" +
          "  \"saveHar\": true,\n" +
          "  \"saveHtml\": true,\n" +
          "  \"saveScreenshot\": true,\n" +
          "  \"url\": \"http://www.${domainName}\",\n" +
          "  \"referer\" : \"https://google.com/\",\n" +
          "  \"screenshotOptions\": {\n" +
          "    \"type\": \"png\",\n" +
          "    \"fullPage\": true,\n" +
          "    \"omitBackground\": false,\n" +
          "    \"encoding\": \"binary\"\n" +
          "  },\n" +
          "  \"browserOptions\": {\n" +
          "    \"headless\": true,\n" +
          "    \"defaultViewport\": {\n" +
          "      \"width\": 1600,\n" +
          "      \"height\": 1200\n" +
          "    }\n" +
          "  }\n" +
          "}";

  enum RequestType {
    VISIT_REQUEST,
    MUPPETS_REQUEST,
    ACK,
    JSON
  }

  // Enum copied from be/dnsbelgium/mercator/common/messaging/ack/CrawlerModule.java
  // because we don't want to depend on that module
  enum CrawlerModule {
    DNS,
    MUPPETS,
    WAPPALYZER,
    SMTP,
    VAT,
    SSL,
    ALL  // to send acks for every module
  }

  public MercatorShell(AmazonSQSAsync amazonSqs) {
    this.amazonSqs = amazonSqs;
  }

  @PostConstruct
  public void init() {
    listConfiguredQueues();
    resolveDispatcherInputQueue();
  }

  private void resolveDispatcherInputQueue() {
    logger.info("dispatcherInputQueue: [{}]", dispatcherInputQueue);
    if (dispatcherInputQueue.isEmpty()) {
      logger.warn("Property dispatcher.input.queue.name is not configured");
    } else {
      logger.info("Calling AWS to get URL of the dispatcher input queue  ...");
      try {
        dispatcherInputQueueUrl = amazonSqs.getQueueUrl(dispatcherInputQueue).getQueueUrl();
        // set dispatcher as default destination at startup
        destinationQueue = dispatcherInputQueue;
        destinationQueueUrl = dispatcherInputQueueUrl;
        logger.info("URL of dispatcher input queue: {}", dispatcherInputQueueUrl);
      } catch (QueueDoesNotExistException e) {
        logger.error("Queue with name {} does not exist", dispatcherInputQueue);
      }
    }
  }

  @ShellMethod("List the names of the configured queues.")
  public void listConfiguredQueues() {
    logger.info("We know about these SQS queues: ");
    for (String queueName : queueNames) {
      logger.info("   " + queueName);
    }
  }

  @ShellMethod("List the names of all SQS queues by calling the AWS SQS API.")
  public void listAwsQueues(@ShellOption(defaultValue = "false") boolean showURLs) {
    ListQueuesResult listQueuesResult = amazonSqs.listQueues();
    logger.info("AWS reports these {} SQS queues: ", listQueuesResult.getQueueUrls().size());
    for (String url : listQueuesResult.getQueueUrls()) {
      if (showURLs) {
        logger.info("  queue with url {}", url);
      } else {
        String name = substringAfterLast(url, "/");
        logger.info("  queue with name {}", name);
      }
    }
  }

  private String substringAfterLast(String input, String search) {
    int index = input.lastIndexOf(search);
    if (index > -1) {
      return input.substring(index + 1);
    }
    return input;
  }

  @ShellMethod("Set given queue as destination for subsequent operations.")
  public void setDestinationQueue(String queueName) {
    logger.info("setting {} as destination queue", queueName);
    logger.info("Calling AWS to get URL of this queue...");
    String url = amazonSqs.getQueueUrl(queueName).getQueueUrl();
    logger.info("URL of this queue: {}", url);
    destinationQueue = queueName;
    destinationQueueUrl = url;
  }

  @ShellMethod("Show the destination for subsequent operations.")
  public void showDestinationQueue() {
    logger.info("Current destination queue is {}", destinationQueue);
    logger.info("URL of this queue: {}", destinationQueueUrl);
    queueSize();
  }

  @ShellMethod("Send an ACK message to the destination queue.")
  @ShellMethodAvailability("destinationQueueSelected")
  public void sendAck(String domainName, String visitId, CrawlerModule crawlerModule) {
    if (crawlerModule == CrawlerModule.ALL) {
      logger.info("Sending an ACK message for every crawler module");
      for (CrawlerModule module: CrawlerModule.values()) {
        if (module != CrawlerModule.ALL) {
          sendRequest(domainName, visitId, RequestType.ACK, module);
        }
      }
    } else {
      logger.info("Sending an ACK message for crawler module {} to {}", crawlerModule, destinationQueue);
      sendRequest(domainName, visitId, RequestType.ACK, crawlerModule);
    }
  }

  /*
    Convenience method for sending a domain name to the dispatcher
   */
  @ShellMethod("Send a domain name to the dispatcher queue")
  public void dispatch(String domainName, @ShellOption(defaultValue = "RANDOM") String visitId) {
    if (dispatcherInputQueueUrl.isEmpty()) {
      logger.error("Dispatcher input queue is not configured");
      return;
    }
    UUID uuid = ("RANDOM".equals(visitId)) ? UUID.randomUUID() : UUID.fromString(visitId);
    String message = generateRequest(domainName, uuid, RequestType.VISIT_REQUEST, null);
    logger.info("Send message: {}", message);
    logger.info("To: {}", dispatcherInputQueueUrl);
    SendMessageResult sendMessageResult =  amazonSqs.sendMessage(dispatcherInputQueueUrl, message);
    logger.info("sendMessageResult: {}", sendMessageResult);
  }

  @ShellMethod("Send a message for given domain name to the destination queue. If you omit a visit_id one will be generated.")
  public void sendRequest(String domainName,
                          @ShellOption(defaultValue = "RANDOM") String visitId,
                          @ShellOption(defaultValue = "VISIT_REQUEST") RequestType requestType,
                          @ShellOption(defaultValue = "") CrawlerModule crawlerModule) {

    UUID uuid = ("RANDOM".equals(visitId)) ? UUID.randomUUID() : UUID.fromString(visitId);
    String message = generateRequest(domainName, uuid, requestType, crawlerModule);

    logger.info("Sending to queue {} with url {}", destinationQueue, destinationQueueUrl);
    logger.info("Message: {}", message);
    SendMessageResult sendMessageResult = sendMessage(message);
    logger.info("sendMessageResult: {}", sendMessageResult);
  }


  private String visitRequest(String domainName, UUID visitId) {
    return VISIT_REQUEST_TEMPLATE
        .replace("${visitId}", visitId.toString())
        .replace("${domain}", domainName);
  }

  @Override
  public AttributedString getPrompt() {
    if (destinationQueue == null) {
      return new AttributedString("no-destination-set:>",
          AttributedStyle.DEFAULT.foreground(AttributedStyle.RED));
    }
    return new AttributedString("destination=" + destinationQueue + ":>",
        AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW));
  }

  @SuppressWarnings("unused")
  public Availability destinationQueueSelected() {
    return (destinationQueueUrl == null)
        ? Availability.unavailable("no valid destination is set.")
        : Availability.available();
  }

  @ShellMethod("Get info about the current destination queue")
  @ShellMethodAvailability("destinationQueueSelected")
  public void getQueueAttributes(boolean all) {
    logger.info("Retrieving attributes for queue with name [{}]", destinationQueue);
    logger.info("URL for this queue:  {}", destinationQueueUrl);
    GetQueueAttributesRequest request = new GetQueueAttributesRequest()
        .withQueueUrl(destinationQueueUrl)
        .withAttributeNames("All");
    GetQueueAttributesResult result = amazonSqs.getQueueAttributes(request);
    logger.info("Info about the queue:");
    for (String key : result.getAttributes().keySet()) {
      logger.info("   {}  = {}", key, result.getAttributes().get(key));
    }
  }

  @ShellMethod("Get approximate number of messages in the destination queue")
  @ShellMethodAvailability("destinationQueueSelected")
  public void getQueueSize() {
    logger.info("Retrieving messages in queue with name [{}]", destinationQueue);
    logger.info("URL for this queue:  {}", destinationQueueUrl);
    queueSize();
  }

  private void queueSize() {
    if (destinationQueueUrl == null) {
      logger.error("First select a valid destination queue");
      return;
    }
    GetQueueAttributesRequest request = new GetQueueAttributesRequest()
        .withQueueUrl(destinationQueueUrl)
        .withAttributeNames("ApproximateNumberOfMessages", "ApproximateNumberOfMessagesNotVisible");
    GetQueueAttributesResult result = amazonSqs.getQueueAttributes(request);
    String approximateNumberOfMessages = result.getAttributes().get("ApproximateNumberOfMessages");
    String approximateNumberOfMessagesInFlight = result.getAttributes().get("ApproximateNumberOfMessagesNotVisible");
    logger.info("Approximate number of messages           : {}", approximateNumberOfMessages);
    logger.info("Approximate number of messages in flight : {}", approximateNumberOfMessagesInFlight);
  }

  @ShellMethod("Convert a file with domain names to a file with visit requests in JSON format")
  public void jsonify(File file, String outputFileName, String label) throws IOException {
    logger.info("Opening scanner on {}. File exists: {}", file.getAbsolutePath(), file.exists());
    Scanner scanner = new Scanner(file);
    FileWriter fileWriter = new FileWriter(outputFileName);
    int counter = 0;
    logger.info("Using label: [{}]", label);
    String template = "{\"domainName\" : \"${domainName}\", \"visitId\" : \"${visitId}\", \"labels\" : [\"${label}\"]}";
    while (scanner.hasNext()) {
      String domainName = scanner.nextLine();
      String visitId = UUID.randomUUID().toString();
      String json = template
          .replace("${domainName}", domainName)
          .replace("${visitId}", visitId)
          .replace("${label}", label);
      fileWriter.append(json).append("\n");
      counter++;
    }
    fileWriter.close();
    logger.info("Written {} visit requests to {}", counter, outputFileName);
  }

  @ShellMethod("Generate a UUID")
  public String uuid() {
    String uuid = randomUUID();
    logger.info("uuid = {}", uuid);
    return uuid;
  }

  private String randomUUID() {
    return UUID.randomUUID().toString();
  }

  private String generateRequest(String domainName, UUID visitId, RequestType requestType, CrawlerModule crawlerModule) {
    switch (requestType) {
      case ACK:
        return ack(domainName, visitId, crawlerModule);
      case MUPPETS_REQUEST:
        return muppetsRequest(domainName, visitId);
      case VISIT_REQUEST:
        return visitRequest(domainName, visitId);
      case JSON:
        // the first parameter should be valid JSON (not
        return domainName;
      default:
        throw new RuntimeException("Unknown RequestType: " + requestType);
    }
  }

  private String ack(String domainName, UUID visitId, CrawlerModule crawlerModule) {
    String ackMessage = ACK_MESSAGE_TEMPLATE
        .replace("${domainName}", domainName)
        .replace("${crawlerModule}", crawlerModule.toString())
        .replace("${visitId}", visitId.toString());
    logger.info("ackMessage = {}", ackMessage);
    return ackMessage;
  }

  private String muppetsRequest(String domainName, UUID visitId) {
    return MUPPETS_REQUEST_TEMPLATE
        .replace("${visitId}", visitId.toString())
        .replace("${domain}", domainName);
  }

  @ShellMethod("Send each line of given file verbatim to the destination queue.")
  @ShellMethodAvailability("destinationQueueSelected")
  public void sendJsonFile(File file) throws FileNotFoundException, InterruptedException {
    sendFile(file, RequestType.JSON, null);
  }

  @ShellMethod("Send for each domain name in given file a request of given type to the destination queue.")
  @ShellMethodAvailability("destinationQueueSelected")
  public void sendFile(File file,
                       @ShellOption(defaultValue = "VISIT_REQUEST") RequestType requestType,
                       @ShellOption(defaultValue = "VAT") CrawlerModule crawlerModule)
      throws FileNotFoundException, InterruptedException {

    logger.info("Generating requests of type {}", requestType);
    logger.info("Opening scanner on {}. File exists: {}", file.getAbsolutePath(), file.exists());
    Scanner scanner = new Scanner(file);
    // Maximum throughput seems to be reached with 65 threads
    // on a m5.large it allows sending 20.000 messages/second
    ExecutorService executorService = Executors.newFixedThreadPool(50);
    long start = System.currentTimeMillis();
    int counter = 0;
    while (scanner.hasNext()) {
      List<String> batch = new ArrayList<>();
      while (batch.size() < 10 && scanner.hasNext()) {
        String line = scanner.nextLine().trim();
        if (requestType == RequestType.JSON) {
          // send the line from the file verbatim to the queue
          batch.add(line);
        } else {
          UUID uuid = UUID.randomUUID();
          String message = generateRequest(line, uuid, requestType, crawlerModule);
          batch.add(message);
        }
        counter++;
      }
      executorService.submit( () -> sendMessages(batch) );
    }
    waitUntilTerminated(executorService, counter);
    long millis = System.currentTimeMillis() - start;
    logger.info("Putting {} messages on the queue took {} ms", counter, millis);
    double ratePerSecond = counter / (millis / 1000.0);
    logger.info("ratePerSecond = " + ratePerSecond);
    logger.info("we are done");
    queueSize();
  }

  @ShellMethod("Receive messages from the queue (messages will become invisible but not deleted).")
  @ShellMethodAvailability("destinationQueueSelected")
  public void readQueue(
      @ShellOption(defaultValue = "1") int maxMessages,
      @ShellOption(defaultValue = "1") int waitTimeSeconds) {

    ReceiveMessageRequest request = new ReceiveMessageRequest()
        .withAttributeNames("All")
        .withQueueUrl(destinationQueueUrl)
        .withWaitTimeSeconds(waitTimeSeconds)
        .withMaxNumberOfMessages(maxMessages);

    ReceiveMessageResult result = amazonSqs.receiveMessage(request);
    logger.info("Messages received: {}", result.getMessages().size());
    for (Message message : result.getMessages()) {
      logger.info("message = {}", message);
    }
  }

  private void sendMessages(List<String> messages) {
    SendMessageBatchRequest request = new SendMessageBatchRequest().withQueueUrl(destinationQueueUrl);
    Map<String, String> map = new HashMap<>();
    int counter = 0;
    for (String message : messages) {
      String id = String.valueOf(counter++);
      map.put(id, message);
      request.getEntries().add(new SendMessageBatchRequestEntry().withId(id).withMessageBody(message));
    }
    SendMessageBatchResult batchResult = amazonSqs.sendMessageBatch(request);
    logger.debug("sent {} messages", batchResult.getSuccessful().size());
    if (!batchResult.getFailed().isEmpty()) {
      logger.warn("Retrying {} messages", batchResult.getFailed().size());
      for (BatchResultErrorEntry errorEntry : batchResult.getFailed()) {
        amazonSqs.sendMessage(destinationQueueUrl, map.get(errorEntry.getId()));
      }
    }
  }

  private void waitUntilTerminated(ExecutorService executorService, int counter) throws InterruptedException {
    logger.info("Submitted {} messages to the thread pool. Now waiting until threads are finished", counter);
    executorService.shutdown();
    while (!executorService.isTerminated()) {
      boolean terminated = executorService.awaitTermination(10, TimeUnit.SECONDS);
      logger.info("executorService.terminated: {}", terminated);
      queueSize();
    }
    logger.info("Done. We have added {} requests to the queue", counter);
  }

  @ShellMethod("Purge all messages from the current destination queue.")
  @ShellMethodAvailability("destinationQueueSelected")
  public void purgeQueue() {
    logger.info("Purging all messages from queue {}", destinationQueueUrl);
    amazonSqs.purgeQueue(new PurgeQueueRequest(destinationQueueUrl));
    queueSize();
  }

  private SendMessageResult sendMessage(String message) {
     return amazonSqs.sendMessage(destinationQueueUrl, message);
  }

}
