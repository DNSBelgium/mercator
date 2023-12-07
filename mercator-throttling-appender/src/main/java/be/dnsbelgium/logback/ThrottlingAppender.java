package be.dnsbelgium.logback;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.AppenderBase;
import ch.qos.logback.core.helpers.CyclicBuffer;
import ch.qos.logback.core.spi.AppenderAttachable;
import ch.qos.logback.core.spi.AppenderAttachableImpl;

import java.util.Iterator;

public class ThrottlingAppender extends AppenderBase<ILoggingEvent> implements AppenderAttachable<ILoggingEvent>  {

//  public enum OnWakeUp {
//    KEEP_BUFFER,
//    EMIT_BUFFER,
//    CLEAR_BUFFER
//  }

  // settings
  private int bufferSize = 512;
  private int maxMessagesToEmitAfterStartUp = 1000;
  private int maxMessagesToEmitAfterWakeUp = 50;
  private int maxSleepMillis = 60_000;

  // onWakeUp not yet implemented
  //private OnWakeUp onWakeUp = OnWakeUp.KEEP_BUFFER;

  // state
  private long lastEmitTimestamp = 0;
  private long numberEventsEmittedSinceWakeUp = 0;
  private long totalEventsEmitted = 0;
  private long totalEventsReceived = 0;
  int appenderCount = 0;
  private final AppenderAttachableImpl<ILoggingEvent> aai = new AppenderAttachableImpl<ILoggingEvent>();

  private AppenderStatus status = AppenderStatus.STARTING;

  protected CyclicBuffer<ILoggingEvent> cyclicBuffer;

  @Override
  protected void append(final ILoggingEvent event) {
    if (!isStarted()) {
      return;
    }
    switch (status) {
      case STARTING -> starting(event);
      case BUFFERING -> buffering(event);
      case AWAKE -> awake(event);
    }
    this.totalEventsReceived++;
  }

  private void info(String msg) {
    addInfo(msg);
  }


  private void changeState(AppenderStatus newState) {
    info("changed from %s to %s".formatted(status, newState));
    this.status = newState;
  }

  private void starting(ILoggingEvent event) {
    if (totalEventsEmitted >= maxMessagesToEmitAfterStartUp) {
      changeState(AppenderStatus.BUFFERING);
      //debug("totalEventsEmitted >= maxMessagesToEmitAfterStartUp => buffering %s", event);
      buffering(event);
    } else {
      //debug("state == Starting => emit %s", event);
      emit(event);
    }
  }

  private void awake(ILoggingEvent event) {
    if (numberEventsEmittedSinceWakeUp >= maxMessagesToEmitAfterWakeUp) {
      numberEventsEmittedSinceWakeUp = 0;
      changeState(AppenderStatus.BUFFERING);
      //debug("emitted enough during wakeUp, start Buffering %s", event);
      buffering(event);
    } else {
      // debug("wakeUp, emit %s", event);
      emit(event);
      numberEventsEmittedSinceWakeUp++;
    }
  }

  private boolean shouldWakeUpFor(ILoggingEvent event) {
    if (maxSleepMillis <= 0) {
      return false;
    }
    return event.getTimeStamp() > lastEmitTimestamp + maxSleepMillis;
  }

  private void buffering(ILoggingEvent event) {
    if (shouldWakeUpFor(event)) {
      changeState(AppenderStatus.AWAKE);
      //debug("was buffering but now awake for %s", event);
      awake(event);
    } else {
      // TODO make threshold configurable
      if (event.getLevel().levelInt >= Level.ERROR_INT) {
        //debug("was buffering but now emitting all buffered events and %s", event);
        emitBuffer();
        emit(event);
      } else {
        //debug("buffering %s", event);
        cyclicBuffer.add(event);
      }
    }
  }

  private void emitBuffer() {
    for (ILoggingEvent event : cyclicBuffer.asList()) {
      emit(event);
    }
    cyclicBuffer.clear();
    //debug("buffer cleared");
  }

  private void emit(ILoggingEvent event) {
    aai.appendLoopOnAppenders(event);
    this.lastEmitTimestamp = event.getTimeStamp();
    this.totalEventsEmitted++;
    //debug("lastEmitTimestamp: %s totalEventsEmitted: %s", lastEmitTimestamp, totalEventsEmitted);
  }

  @Override
  public final void start() {
    if (isStarted()) {
      return;
    }
    int errors = 0;
    if (bufferSize <= 1) {
      addError("Invalid bufferSize [" + bufferSize + "]. Aborting");
      errors++;
    }
    if (maxSleepMillis == 0) {
      addInfo("maxSleepMillis == 0 => After the startup messages the appender will only wake up on errors");
    }
    if (maxSleepMillis > 0 && maxMessagesToEmitAfterWakeUp <= 0) {
      addError("maxMessagesToEmitAfterWakeUp must be greater than zero. Aborting");
      errors++;
    }
    if (maxMessagesToEmitAfterStartUp < 0) {
      addError("maxMessagesToEmitAfterWakeUp must be greater than or equal to zero. Aborting");
      errors++;
    }
    if (appenderCount <= 0) {
      addError("No attached appenders found. Aborting");
      errors++;
    }
    if (errors == 0) {
      cyclicBuffer = new CyclicBuffer<>(bufferSize);
      super.start();
    }
  }

  public void addAppender(Appender<ILoggingEvent> newAppender) {
    String className = ThrottlingAppender.class.getCanonicalName();
    if (appenderCount == 0) {
      appenderCount++;
      addInfo("Attaching appender named [" + newAppender.getName() + "] to " + className);
      aai.addAppender(newAppender);
    } else {
      addWarn("One and only one appender may be attached to " + className);
      addWarn("Ignoring additional appender named [" + newAppender.getName() + "]");
    }
  }

  @Override
  public Iterator<Appender<ILoggingEvent>> iteratorForAppenders() {
    return aai.iteratorForAppenders();
  }

  @Override
  public Appender<ILoggingEvent> getAppender(String name) {
    return aai.getAppender(name);
  }

  @Override
  public boolean isAttached(Appender<ILoggingEvent> appender) {
    return aai.isAttached(appender);
  }

  @Override
  public void detachAndStopAllAppenders() {
    aai.detachAndStopAllAppenders();
  }

  @Override
  public boolean detachAppender(Appender<ILoggingEvent> appender) {
    return aai.detachAppender(appender);
  }

  @Override
  public boolean detachAppender(String name) {
    return aai.detachAppender(name);
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
  }

  public int getMaxMessagesToEmitAfterStartUp() {
    return maxMessagesToEmitAfterStartUp;
  }

  public void setMaxMessagesToEmitAfterStartUp(int maxMessagesToEmitAfterStartUp) {
    this.maxMessagesToEmitAfterStartUp = maxMessagesToEmitAfterStartUp;
  }

  public int getMaxSleepMillis() {
    return maxSleepMillis;
  }

  public void setMaxSleepMillis(int maxSleepMillis) {
    this.maxSleepMillis = maxSleepMillis;
  }

  public int getMaxMessagesToEmitAfterWakeUp() {
    return maxMessagesToEmitAfterWakeUp;
  }

  public void setMaxMessagesToEmitAfterWakeUp(int maxMessagesToEmitAfterWakeUp) {
    this.maxMessagesToEmitAfterWakeUp = maxMessagesToEmitAfterWakeUp;
  }

  public AppenderStatus getStatus() {
    return status;
  }

  public long getTotalEventsReceived() {
    return totalEventsReceived;
  }

  public long getTotalEventsEmitted() {
    return totalEventsEmitted;
  }

//  public OnWakeUp getOnWakeUp() {
//    return onWakeUp;
//  }
//
//  public void setOnWakeUp(OnWakeUp onWakeUp) {
//    addInfo("onWakeUp: " + onWakeUp);
//    this.onWakeUp = onWakeUp;
//  }
}