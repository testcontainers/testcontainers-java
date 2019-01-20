# Accessing container logs

It is possible to capture container output using the `followOutput()` method. This method accepts a Consumer and (optionally)
a varargs list stating which of STDOUT, STDERR, or both, should be followed. If not specified, both will be followed.

At present, container output will always begin from the time of container creation.

Testcontainers includes some out-of-the-box Consumer implementations that can be used; examples follow.

## Streaming container output to an SLF4J logger

Given an existing SLF4J logger instance named LOGGER:
```java
Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LOGGER);
container.followOutput(logConsumer);
```

## Capturing container output as a String
```java
ToStringConsumer toStringConsumer = new ToStringConsumer();
container.followOutput(toStringConsumer, OutputType.STDOUT);

String utf8String = toStringConsumer.toUtf8String();

// Or if the container output is not UTF-8
String otherString = toStringConsumer.toString(CharSet.forName("ISO-8859-1"));
```

## Waiting for container output to contain expected content

`WaitingConsumer` will block until a frame of container output (usually a line) matches a provided predicate.

A timeout may be specified, as shown in this example.
```java
WaitingConsumer consumer = new WaitingConsumer();

container.followOutput(consumer, STDOUT);

consumer.waitUntil(frame -> 
    frame.getUtf8String().contains("STARTED"), 30, TimeUnit.SECONDS);
```

Additionally, as the Java 8 Consumer functional interface is used, Consumers may be composed together. This is
useful, for example, to capture all the container output but only when a matching string has been found. e.g.:
```java
WaitingConsumer waitingConsumer = new WaitingConsumer();
ToStringConsumer toStringConsumer = new ToStringConsumer();

Consumer<OutputFrame> composedConsumer = toStringConsumer.andThen(waitingConsumer);
container.followOutput(composedConsumer);

waitingConsumer.waitUntil(frame -> 
    frame.getUtf8String().contains("STARTED"), 30, TimeUnit.SECONDS);

String utf8String = toStringConsumer.toUtf8String();
```
