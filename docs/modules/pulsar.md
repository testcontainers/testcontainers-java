# Apache Pulsar Module

Testcontainers can be used to automatically create [Apache Pulsar](https://pulsar.apache.org) containers without external services.

It's based on the official Apache Pulsar docker image, it is recommended to read the [official guide](https://pulsar.apache.org/docs/next/getting-started-docker/).

## Example

Create a `PulsarContainer` to use it in your tests:

<!--codeinclude-->
[Create a Pulsar container](../../modules/pulsar/src/test/java/org/testcontainers/containers/PulsarContainerTest.java) inside_block:constructorWithVersion
<!--/codeinclude-->

Then you can retrieve the broker and the admin url:

<!--codeinclude-->
[Get broker and admin urls](../../modules/pulsar/src/test/java/org/testcontainers/containers/PulsarContainerTest.java) inside_block:coordinates
<!--/codeinclude-->

## Options

### Configuration
If you need to set Pulsar configuration variables you can use the native APIs and set each variable with `PULSAR_PREFIX_` as prefix.

For example, if you want to enable `brokerDeduplicationEnabled`:

<!--codeinclude-->
[Set configuration variables](../../modules/pulsar/src/test/java/org/testcontainers/containers/PulsarContainerTest.java) inside_block:constructorWithEnv
<!--/codeinclude-->

### Pulsar IO

If you need to test Pulsar IO framework you can enable the Pulsar Functions Worker:

<!--codeinclude-->
[Create a Pulsar container with functions worker](../../modules/pulsar/src/test/java/org/testcontainers/containers/PulsarContainerTest.java) inside_block:constructorWithFunctionsWorker
<!--/codeinclude-->

### Pulsar Transactions

If you need to test Pulsar Transactions you can enable the transactions feature:

<!--codeinclude-->
[Create a Pulsar container with transactions](../../modules/pulsar/src/test/java/org/testcontainers/containers/PulsarContainerTest.java) inside_block:constructorWithTransactions
<!--/codeinclude-->


## Adding this module to your project dependencies

Add the following dependency to your `pom.xml`/`build.gradle` file:

=== "Gradle"
    ```groovy
    testImplementation "org.testcontainers:pulsar:{{latest_version}}"
    ```
=== "Maven"
    ```xml
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>pulsar</artifactId>
        <version>{{latest_version}}</version>
        <scope>test</scope>
    </dependency>
    ```
