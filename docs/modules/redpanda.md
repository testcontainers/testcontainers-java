# Redpanda

Testcontainers can be used to automatically instantiate and manage [Redpanda](https://redpanda.com/) containers.
More precisely Testcontainers uses the official Docker images for [Redpanda](https://hub.docker.com/r/redpandadata/redpanda)

!!! note
    This module uses features provided in `docker.redpanda.com/redpandadata/redpanda`.

## Example

Create a `Redpanda` to use it in your tests:
<!--codeinclude-->
[Creating a Redpanda](../../modules/redpanda/src/test/java/org/testcontainers/redpanda/RedpandaContainerTest.java) inside_block:constructorWithVersion
<!--/codeinclude-->

Now your tests or any other process running on your machine can get access to running Redpanda broker by using the following bootstrap server location:

<!--codeinclude-->
[Bootstrap Servers](../../modules/redpanda/src/test/java/org/testcontainers/redpanda/RedpandaContainerTest.java) inside_block:getBootstrapServers
<!--/codeinclude-->

Redpanda also provides a schema registry implementation. Like the Redpanda broker, you can access by using the following schema registry location:

<!--codeinclude-->
[Schema Registry](../../modules/redpanda/src/test/java/org/testcontainers/redpanda/RedpandaContainerTest.java) inside_block:getSchemaRegistryAddress
<!--/codeinclude-->

It is also possible to enable security capabilities of Redpanda by using:

<!--codeinclude-->
[Enable security](../../modules/redpanda/src/test/java/org/testcontainers/redpanda/RedpandaContainerTest.java) inside_block:security
<!--/codeinclude-->

Superusers can be created by using:

<!--codeinclude-->
[Register Superuser](../../modules/redpanda/src/test/java/org/testcontainers/redpanda/RedpandaContainerTest.java) inside_block:createSuperUser
<!--/codeinclude-->

Below is an example of how to create the `AdminClient`:

<!--codeinclude-->
[Create Admin Client](../../modules/redpanda/src/test/java/org/testcontainers/redpanda/RedpandaContainerTest.java) inside_block:createAdminClient
<!--/codeinclude-->

There are scenarios where additional listeners are needed because the consumer/producer can be another
container in the same network or a different process where the port to connect differs from the default
exposed port `9092`. E.g [Toxiproxy](../../docs/modules/toxiproxy.md).

<!--codeinclude-->
[Register additional listener](../../modules/redpanda/src/test/java/org/testcontainers/redpanda/RedpandaContainerTest.java) inside_block:registerListener
<!--/codeinclude-->

Container defined in the same network:

<!--codeinclude-->
[Create kcat container](../../modules/redpanda/src/test/java/org/testcontainers/redpanda/RedpandaContainerTest.java) inside_block:createKCatContainer
<!--/codeinclude-->

Client using the new registered listener:

<!--codeinclude-->
[Produce/Consume via new listener](../../modules/redpanda/src/test/java/org/testcontainers/redpanda/RedpandaContainerTest.java) inside_block:produceConsumeMessage
<!--/codeinclude-->

The following examples shows how to register a proxy as a new listener in `RedpandaContainer`:

Use `SocatContainer` to create the proxy

<!--codeinclude-->
[Create Proxy](../../modules/redpanda/src/test/java/org/testcontainers/redpanda/RedpandaContainerTest.java) inside_block:createProxy
<!--/codeinclude-->

Register the listener and advertised listener

<!--codeinclude-->
[Register Listener](../../modules/redpanda/src/test/java/org/testcontainers/redpanda/RedpandaContainerTest.java) inside_block:registerListenerAndAdvertisedListener
<!--/codeinclude-->

Client using the new registered listener:

<!--codeinclude-->
[Produce/Consume via new listener](../../modules/redpanda/src/test/java/org/testcontainers/redpanda/RedpandaContainerTest.java) inside_block:produceConsumeMessageFromProxy
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
