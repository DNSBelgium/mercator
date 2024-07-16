package be.dnsbelgium.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Random;

import static be.dnsbelgium.logback.AppenderStatus.BUFFERING;
import static be.dnsbelgium.logback.AppenderStatus.STARTING;
import static org.assertj.core.api.Assertions.assertThat;

class ThrottlingAppenderTest {

  ListAppender<ILoggingEvent> listAppender;
  ThrottlingAppender throttlingAppender;

  private LoggerContext loggerContext;

  @BeforeEach
  void beforeEach() {
    loggerContext = new LoggerContext();
    listAppender = new ListAppender<>();
    listAppender.setName("listAppender");
    listAppender.setContext(loggerContext);
    throttlingAppender = new ThrottlingAppender();
    throttlingAppender.setContext(loggerContext);
    throttlingAppender.addAppender(listAppender);
  }

  @Test
  void shouldStartBufferingWhenMaxMessagesToEmitAfterStartUpIsReached() {
    throttlingAppender.setMaxMessagesToEmitAfterStartUp(3);
    listAppender.start();
    throttlingAppender.start();
    assertThat(throttlingAppender.getStatus()).isEqualTo(STARTING);
    long timestamp = 1;
    String msg = "Some message";
    LoggingEvent event1 = makeEvent(timestamp++, msg, timestamp);
    LoggingEvent event2 = makeEvent(timestamp++, msg, timestamp);
    LoggingEvent event3 = makeEvent(timestamp++, msg, timestamp);
    LoggingEvent event4 = makeEvent(timestamp++, msg, timestamp);
    LoggingEvent event5 = makeEvent(timestamp++, msg, timestamp);
    throttlingAppender.append(event1);
    throttlingAppender.append(event2);
    throttlingAppender.append(event3);
    throttlingAppender.append(event4);
    throttlingAppender.append(event5);
    System.out.println("listAppender.list.size() = " + listAppender.list.size());
    for (ILoggingEvent event : listAppender.list) {
      System.out.println("event = " + event);
    }
    assertThat(listAppender.list).hasSize(3);
    assertThat(listAppender.list).containsExactly(event1, event2, event3);
    assertThat(throttlingAppender.getStatus()).isEqualTo(BUFFERING);
  }

  @Test void shouldEmitBufferWhenAppendingError() {
    throttlingAppender.setMaxMessagesToEmitAfterStartUp(0);
    throttlingAppender.setBufferSize(5);
    // disable wake-ups
    throttlingAppender.setMaxSleepMillis(0);
    listAppender.start();
    throttlingAppender.start();
    var appended = new ArrayList<LoggingEvent>();
    for (int i = 0; i < 500; ++i) {
      LoggingEvent event = event(i);
      throttlingAppender.append(event);
      appended.add(event);
    }
    var error1 = errorEvent(600, "this is an error");
    var error2 = errorEvent(601, "this is another error");
    throttlingAppender.append(error1);
    throttlingAppender.append(error2);

    System.out.println("listAppender.list.size() = " + listAppender.list.size());
    for (ILoggingEvent event : listAppender.list) {
      System.out.println("event = " + event);
    }
    assertThat(listAppender.list).hasSize(7);
    var expected = new ArrayList<LoggingEvent>();
    // The last 5 messages and the errors should have been emitted
    for (int i = 495; i < 500; ++i) {
      expected.add( appended.get(i) );
    }
    expected.add(error1);
    expected.add(error2);
    assertThat(listAppender.list).isEqualTo(expected);
  }

  @Test void maxMessagesToEmitAfterWakeUpMustBeGreaterThanZero() {
    throttlingAppender.setMaxSleepMillis(1);
    throttlingAppender.setMaxMessagesToEmitAfterWakeUp(0);
    throttlingAppender.start();
    loggerContext.getStatusManager().getCopyOfStatusList().forEach(System.out::println);
    assertThat(throttlingAppender.isStarted()).isFalse();
  }

  @Test void bufferSizeMustBeGreaterThanOne() {
    throttlingAppender.setBufferSize(0);
    throttlingAppender.start();
    loggerContext.getStatusManager().getCopyOfStatusList().forEach(System.out::println);
    assertThat(throttlingAppender.isStarted()).isFalse();

    loggerContext.getStatusManager().clear();
    throttlingAppender.setBufferSize(1);
    throttlingAppender.start();
    loggerContext.getStatusManager().getCopyOfStatusList().forEach(System.out::println);
    assertThat(throttlingAppender.isStarted()).isFalse();

    throttlingAppender.setBufferSize(2);
    throttlingAppender.start();
    assertThat(throttlingAppender.isStarted()).isTrue();

  }

  @Test
  public void wakeUps() {
    for (int i=0; i<50; i++) {
      beforeEach();
      wakeUp();
    }
  }

  @Test public void wakeUp() {
    // set up
    var random = new Random();
    int SLEEP_MILLIS = 2 + random.nextInt(50);
    int MILLIS_TO_TEST = 100 + random.nextInt(200);
    int MAX_AFTER_START_UP = 1 + random.nextInt(10);
    int MAX_EVENTS_AFTER_WAKE_UP = 1 + random.nextInt(10);
    // every millisecond append some events
    int EVENTS_PER_MILLISECOND = 17;

    System.out.println("int SLEEP_MILLIS = " + SLEEP_MILLIS + ";");
    System.out.println("int MILLIS_TO_TEST = " + MILLIS_TO_TEST + ";");
    System.out.println("int MAX_AFTER_START_UP = " + MAX_AFTER_START_UP + ";");
    System.out.println("int MAX_EVENTS_AFTER_WAKE_UP = " + MAX_EVENTS_AFTER_WAKE_UP + ";");

    throttlingAppender.setMaxMessagesToEmitAfterStartUp(MAX_AFTER_START_UP);
    throttlingAppender.setMaxMessagesToEmitAfterWakeUp(MAX_EVENTS_AFTER_WAKE_UP);
    throttlingAppender.setMaxSleepMillis(SLEEP_MILLIS);
    listAppender.start();
    throttlingAppender.start();

    // Every millisecond we append EVENTS_PER_MILLISECOND events
    // Suppose MAX_AFTER_START_UP = 1 and SLEEP_MILLIS = 5
    // Then the very first message (at timestamp 1) should be emitted, then the appender should start buffering.
    // At timestamp 7 the appender should wake up and emit two messages, after which it will start buffering again.
    // At timestamp 13 the appender will have slept for 5 millis, and it will wake up again.
    // => all emitted events should have a timestamp that is 1 mod 6 (7, 13, 19, 25, ...)
    int totalEmitted = 0;

    for (int timestamp=1; timestamp < MILLIS_TO_TEST; timestamp++) {
      for (int k=1; k <= EVENTS_PER_MILLISECOND; k++) {
        var event = makeEvent(timestamp, "message {} at ts {}", k, timestamp);
        boolean isWithinMaxAfterWakeUp  = (k <= MAX_EVENTS_AFTER_WAKE_UP);
        boolean isStartUpMessage = (totalEmitted < MAX_AFTER_START_UP);
        boolean isAfterFirstSleep = (timestamp > SLEEP_MILLIS);
        boolean isOnWakeUpTime = timestamp % (SLEEP_MILLIS + 1) == 1;

        throttlingAppender.append(event);

        boolean shouldHaveBeenEmitted =
            (isStartUpMessage) || (isAfterFirstSleep && isOnWakeUpTime && isWithinMaxAfterWakeUp);

        boolean emitted = listAppender.list.contains(event);
        if (emitted) {
          totalEmitted++;
        }
        if (emitted != shouldHaveBeenEmitted) {
          System.out.println("timestamp = " + timestamp);
          System.out.println("k = " + k);
          System.out.println("event = " + event);
          System.out.println("emitted = " + emitted);
          System.out.println("isStartUpMessage = " + isStartUpMessage);
          System.out.println("isOnWakeUpTime = " + isOnWakeUpTime);
          System.out.println("isAfterFirstSleep = " + isAfterFirstSleep);
          System.out.println("isWithinMaxAfterWakeUp = " + isWithinMaxAfterWakeUp);
          System.out.println("shouldHaveBeenEmitted = " + shouldHaveBeenEmitted);
        }
        assertThat(emitted).isEqualTo(shouldHaveBeenEmitted);
      }
    }
    long wakeUps = loggerContext.getStatusManager().getCopyOfStatusList()
        .stream()
        .filter(s -> s.getMessage().contains("changed from BUFFERING to AWAKE"))
        .count();

    // wake up at 7, 13, 19, ... 37 => 6 times
    int numberOfSleeps = MILLIS_TO_TEST / (SLEEP_MILLIS + 1);
    System.out.println("numberOfSleeps = " + numberOfSleeps);
    int lastWakeUp = numberOfSleeps * (SLEEP_MILLIS + 1) + 1;
    System.out.println("lastWakeUp = " + lastWakeUp);
    int expectedWakeUps = (MILLIS_TO_TEST > lastWakeUp) ? numberOfSleeps : numberOfSleeps - 1;

    // int expectedWakeUps = (MILLIS_TO_TEST - 1)  / (SLEEP_MILLIS + 1);

    System.out.println("wakeUps = " + wakeUps);
    System.out.println("expectedWakeUps = " + expectedWakeUps);
    assertThat(wakeUps).isEqualTo(expectedWakeUps);

  }

  private LoggingEvent event(long timestamp) {
    LoggingEvent event = new LoggingEvent();
    event.setMessage("This is message " + timestamp);
    event.setTimeStamp(timestamp);
    event.setLevel(Level.INFO);
    return event;
  }


  private LoggingEvent makeEvent(long timestamp, String msg, Object... args) {
    LoggingEvent event = new LoggingEvent();
    event.setMessage(msg);
    event.setTimeStamp(timestamp);
    event.setLevel(Level.INFO);
    event.setArgumentArray(args);
    return event;
  }

  private LoggingEvent errorEvent(long timestamp, String msg) {
    LoggingEvent event = new LoggingEvent();
    event.setMessage(msg);
    event.setTimeStamp(timestamp);
    event.setLevel(Level.ERROR);
    return event;
  }

}