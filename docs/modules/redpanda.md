# Redpanda

Testcontainers can be used to automatically instantiate and manage [Redpanda](https://redpanda.com/) containers.
More precisely Testcontainers uses the official Docker images for [Redpanda](https://hub.docker.com/r/vectorized/redpanda/)

!!! note
    This module uses features provided in `docker.redpanda.com/vectorized/redpanda`.

## Example

Create a `Redpanda` to use it in your tests:
<!--codeinclude-->
[Creating a Redpanda](../../modules/redpanda/src/test/java/org/testcontainers/redpanda/RedpandaContainerTest.java) inside_block:constructorWithVersion
<!--/codeinclude-->

Now your tests or any other process running on your machine can get access to running Redpanda broker by using the following bootstrap server location:

<!--codeinclude-->
[Bootstrap Servers](../../modules/redpanda/src/test/java/org/testcontainers/redpanda/RedpandaContainerTest.java) inside_block:getBootstrapServers
<!--/codeinclude-->

## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
```groovy
testImplementation "org.testcontainers:redpanda:{{latest_version}}"
```
=== "Maven"
```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>redpanda</artifactId>
    <version>{{latest_version}}</version>
    <scope>test</scope>
</dependency>
```
