# NATS Module

## Usage example

This example connects to the NATS server and asserts the connection status of the client.

<!--codeinclude-->
[Using a NATS container](../../modules/nats/src/test/java/org/testcontainers/containers/NATSContainerTest.java) inside_block:natsContainerUsage
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

```groovy tab='Gradle'
testCompile "org.testcontainers:nats:{{latest_version}}"
```

```xml tab='Maven'
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>nats</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
