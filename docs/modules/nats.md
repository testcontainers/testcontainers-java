# NATS Module

!!! note
    This module is INCUBATING. While it is ready for use and operational in the current version of Testcontainers, it is possible that it may receive breaking changes in the future. See [our contributing guidelines](/contributing/#incubating-modules) for more information on our incubating modules policy.

[NATS](https://nats.io) is a simple, secure and high performance open source messaging system for cloud native applications, IoT messaging, and microservices architectures.

## Example

Create a `NatsContainer` to use it in your tests:

<!--codeinclude-->
[Creating a NatsContainer](../../modules/nats/src/test/java/org/testcontainers/nats/NatsContainerTest.java) inside_block:shouldStartNatsContainer
<!--/codeinclude-->

Now your tests can connect to NATS using the connection URL:

<!--codeinclude-->
[Connecting to NATS](../../modules/nats/src/test/java/org/testcontainers/nats/NatsContainerTest.java) inside_block:shouldPublishAndSubscribeMessages
<!--/codeinclude-->

## Options

### Using JetStream

JetStream is NATS' built-in distributed persistence system. You can enable it easily:

<!--codeinclude-->
[Enabling JetStream](../../modules/nats/src/test/java/org/testcontainers/nats/NatsContainerTest.java) inside_block:shouldSupportJetStream
<!--/codeinclude-->

### Using Authentication

You can configure NATS to require username and password authentication:

<!--codeinclude-->
[Using Authentication](../../modules/nats/src/test/java/org/testcontainers/nats/NatsContainerTest.java) inside_block:shouldSupportAuthentication
<!--/codeinclude-->

### Enabling Debug/Trace Logging

For debugging purposes, you can enable verbose logging:

```java
NatsContainer nats = new NatsContainer(DockerImageName.parse("nats:2.10"))
    .withDebug()             // Enable debug logging
    .withProtocolTracing();  // Enable protocol tracing
```

## Accessing Monitoring

NATS provides an HTTP monitoring endpoint that you can access:

```java
String httpMonitoringUrl = natsContainer.getHttpMonitoringUrl();
```

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:testcontainers-nats:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers-nats</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```