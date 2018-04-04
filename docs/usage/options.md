# Options

### Image

With a generic container, you set the container image using a parameter to the rule constructor, e.g.:
```java
new GenericContainer("jboss/wildfly:9.0.1.Final")
```

### Exposing ports

If you need to expose ports on the container, use the `withExposedPorts` method on the rule passing integer port numbers:
```java
new GenericContainer(...)
		.withExposedPorts(22, 80, 8080)
```

or strings (optionally specifying tcp):
```java
new GenericContainer(...)
		.withExposedPorts("22", "80/tcp", "8080/tcp")
```

### Environment variables

To add environment variables to the container, use `withEnv`:
```java
new GenericContainer(...)
		.withEnv("API_TOKEN", "foo")
```

### Command

By default the container will execute whatever command is specified in the image's Dockerfile. To override this, and specify a different command, use `withCommand`:
```java
new GenericContainer(...)
        .withCommand("/app/start.sh")
```

### Volume mapping

It is possible to map a file or directory **on the classpath** into the container as a volume using `withClasspathResourceMapping`:
```java
new GenericContainer(...)
        .withClasspathResourceMapping("redis.conf",
                                      "/etc/redis.conf",
                                      BindMode.READ_ONLY)
```

### Customizing the container

It is possible to use the [`docker-java`](https://github.com/docker-java/docker-java) API directly to customize containers before creation. This is useful if there is a need to use advanced Docker features that are not exposed by the Testcontainers API. Any customizations you make using `withCreateContainerCmdModifier` will be applied _on top_ of the container definition that Testcontainers creates, but before it is created.

For example, this can be used to change the container hostname:
```java
new GenericContainer<>("redis:3.0.2")
        .withCreateContainerCmdModifier(cmd -> cmd.withHostName("the-cache"))
        ...
```

... or modify container memory (see [this](https://fabiokung.com/2014/03/13/memory-inside-linux-containers/) if it does not appear to work):
```java
new GenericContainer<>("alpine:3.3")
        .withCreateContainerCmdModifier(cmd -> cmd.withMemory((long) 4 * 1024 * 1024))
        .withCreateContainerCmdModifier(cmd -> cmd.withMemorySwap((long) 4 * 1024 * 1024))
        ...
```

> It is recommended to use this sparingly, and follow changes to the `docker-java` API if you choose to use this. It is typically quite stable, though.

For what is possible, consult the [`docker-java CreateContainerCmd` source code](https://github.com/docker-java/docker-java/blob/master/src/main/java/com/github/dockerjava/api/command/CreateContainerCmd.java)




### Startup timeout

Ordinarily Testcontainers will wait for up to 60 seconds for the container's first mapped network port to start listening.

This simple measure provides a basic check whether a container is ready for use.

If the default 60s timeout is not sufficient, it can be altered with the `withStartupTimeout()` method.

If waiting for a listening TCP port is not sufficient to establish whether the container is ready, you can use the
`waitingFor()` method with other `WaitStrategy` implementations as shown below.

#### Waiting for startup examples

You can choose to wait for an HTTP(S) endpoint to return a particular status code.

Waiting for 200 OK:
```java
@ClassRule
public static GenericContainer elasticsearch =
    new GenericContainer("elasticsearch:2.3")
               .withExposedPorts(9200)
               .waitingFor(Wait.forHttp("/all"));
```

Wait for arbitrary status code on an HTTPS endpoint:
```java
@ClassRule
public static GenericContainer elasticsearch =
    new GenericContainer("elasticsearch:2.3")
               .withExposedPorts(9200)
               .waitingFor(
               		Wait.forHttp("/all")
               			 .forStatusCode(301)
               			 .usingTls());
 ```

If the used image supports Docker's [Healthcheck](https://docs.docker.com/engine/reference/builder/#healthcheck) feature, you can directly leverage the `healthy` state of the container as your wait condition:
```java
@ClassRule2.32.3
public static GenericContainer container =
    new GenericContainer("image-with-healthcheck:4.2")
               .waitingFor(Wait.forHealthcheck());
```

For futher options, check out the `Wait` convenience class, or the various subclasses of `WaitStrategy`. If none of these options
meet your requirements, you can create your own subclass of `AbstractWaitStrategy` with an appropriate wait
mechanism in `waitUntilReady()`. The `GenericContainer.waitingFor()` method accepts any valid `WaitStrategy`.

### Following container output

It is possible to capture container output using the `followOutput()` method. This method accepts a Consumer and (optionally)
a varargs list stating which of STDOUT, STDERR, or both, should be followed. If not specified, both will be followed.

At present, container output will always begin from the time of container creation.

Testcontainers includes some out-of-the-box Consumer implementations that can be used; examples follow.

#### Streaming container output to an SLF4J logger

Given an existing SLF4J logger instance named LOGGER:
```java
Slf4jLogConsumer logConsumer = new Slf4jLogConsumer(LOGGER);
container.followOutput(logConsumer);
```

#### Capturing container output as a String
```java
ToStringConsumer toStringConsumer = new ToStringConsumer();
container.followOutput(toStringConsumer, OutputType.STDOUT);

String utf8String = toStringConsumer.toUtf8String();

// Or if the container output is not UTF-8
String otherString = toStringConsumer.toString(CharSet.forName("ISO-8859-1"));
```

#### Waiting for container output to contain expected content

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

### Executing a command

Your test can execute a command inside a running container, similar to a `docker exec` call:
```java
myContainer.execInContainer("touch", "/tmp/foo");
```

This can be useful for software that has a command line administration tool. You can also get the output from the command:
```java
ExecResult result = myContainer.execInContainer("tail", "-1", "/var/logs/foo");
assertThat(result.getStdout().contains("message"));
```

There are two limitations:
* There's no way to get the return code of the executed command
* This isn't supported if your docker daemon uses the older "lxc" execution engine.
