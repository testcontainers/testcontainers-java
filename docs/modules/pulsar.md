# Apache Pulsar Module

Testcontainers can be used to automatically create [Apache Pulsar](https://pulsar.apache.org) containers without external services.


## Example

Create a `PulsarContainer` to use it in your tests:

<!--codeinclude-->
[Create a Pulsar container](../../modules/pulsar/src/test/java/org/testcontainers/containers/PulsarContainerTest.java) inside_block:constructorWithVersion
<!--/codeinclude-->

Then you can retrieve the broker and the admin url:

<!--codeinclude-->
[Get broker and admin urls](../../modules/pulsar/src/test/java/org/testcontainers/containers/PulsarContainerTest.java) inside_block:coordinates
<!--/codeinclude-->

If you need to test Pulsar IO framework you can enable the Pulsar Functions Worker:

<!--codeinclude-->
[Create a Pulsar container with functions worker](../../modules/pulsar/src/test/java/org/testcontainers/containers/PulsarContainerTest.java) inside_block:constructorWithFunctionsWorker
<!--/codeinclude-->


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
