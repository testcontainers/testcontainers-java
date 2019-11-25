# Accessing container logs

It is possible to capture container output using:
 
 * the `getLogs()` method, which simply returns a `String` snapshot of a container's entire log output
 * the `followOutput()` method. This method accepts a Consumer and (optionally)
a varargs list stating which of STDOUT, STDERR, or both, should be followed. If not specified, both will be followed.

At present, container output will always begin from the time of container creation.

## Reading all logs (from startup time to present)

`getLogs()` is the simplest mechanism for accessing container logs, and can be used as follows:

<!--codeinclude--> 
[Accessing all output (stdout and stderr)](../../core/src/test/java/org/testcontainers/containers/output/ContainerLogsTest.java) inside_block:docsGetAllLogs
<!--/codeinclude-->

<!--codeinclude--> 
[Accessing just stdout](../../core/src/test/java/org/testcontainers/containers/output/ContainerLogsTest.java) inside_block:docsGetStdOut
<!--/codeinclude-->

<!--codeinclude--> 
[Accessing just stderr](../../core/src/test/java/org/testcontainers/containers/output/ContainerLogsTest.java) inside_block:docsGetStdErr
<!--/codeinclude-->

## Streaming logs

Testcontainers includes some out-of-the-box Consumer implementations that can be used with the streaming `followOutput()` model; examples follow.

### Streaming container output to an SLF4J logger

Given an existing SLF4J logger instance named LOGGER:
```java
Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LOGGER);
container.followOutput(logConsumer);
```

By default both standard out and standard error will both be emitted at INFO level. 
Standard error may be emitted at ERROR level, if desired:

```java
Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log).withSeparateOutputStreams()
```

The [Mapped Diagnostic Context (MDC)](http://logback.qos.ch/manual/mdc.html) for emitted messages may be configured using the `withMdc(...)` option:

```java
Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log).withMdc("key", "value")
```

or using an existing map of key-value pairs:

```java
Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(log).withMdc(map)
```

### Capturing container output as a String

To stream logs live or customize the decoding, `ToStringConsumer` may be used:

```java
ToStringConsumer toStringConsumer = new ToStringConsumer();
container.followOutput(toStringConsumer, OutputType.STDOUT);

String utf8String = toStringConsumer.toUtf8String();

// Or if the container output is not UTF-8
String otherString = toStringConsumer.toString(CharSet.forName("ISO-8859-1"));
```

### Waiting for container output to contain expected content

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
