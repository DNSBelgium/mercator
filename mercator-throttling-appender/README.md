# Logback Throttling appender

An appender that can be throttled and delegates the appending itself to another appender.

## Features

This appender will emit a configurable number of messages at start-up and then go silent.
While the appender is silent it will buffer a configurable number of messages in memory.
When the appender sees a log message of level >= ERROR, it will emit all the messages in the buffer.

Every 60 seconds, the appender will wake up and emit a configurable number of messages and go silent again.

This is suitable for long-running applications that emit MANY, MANY messages that nobody cares about
unless an error occurs.
And waking up every minute can be handy to make sure to application is still doing something.

TODO: make the wake-up interval configurable.

## Requirements

- Java 17
- Logback 1.2.9


## Examples

Simple configuration:

```xml
<configuration>

    <appender name="listAppender" class="ch.qos.logback.core.read.ListAppender"/>

    <appender name="ThrottlingAppender" class="be.dnsbelgium.logback.ThrottlingAppender">
        <bufferSize>10</bufferSize>
        <maxMessagesToEmitAfterStartUp>6</maxMessagesToEmitAfterStartUp>
        <maxMessagesToEmitAfterWakeUp>3</maxMessagesToEmitAfterWakeUp>
        <appender-ref ref="listAppender" />
    </appender>

    <root level="debug">
        <appender-ref ref="Throttle" />
    </root>

</configuration>
```

For more examples, see [./src/test/resources/](./src/test/resources/)
